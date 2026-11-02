import { useApolloClient, useMutation, useQuery, type ApolloError } from "@apollo/client";
import { Banner, Button, DataTable, Download, EmptyData, Header, LayoutContent, Loader, Paper, Reload, Report as ReportIcon, Typography } from "@jahia/moonstone";
import type { DataTableColumn } from "@jahia/moonstone/DataTable";
import { useEffect, useMemo, useState } from "react";
import { I18nextProvider, useTranslation } from "react-i18next";
import { i18n, NAMESPACE } from "../i18n.ts";
import { MOONSTONE_SCOPE_ATTRIBUTE } from "../moonstoneScope.ts";
import type { FilterId } from "./FilterChip.tsx";
import {
  REPORT_CSV_QUERY,
  REPORT_QUERY,
  RUN_REPORT_MUTATION,
  type Report,
  type ReportCsvData,
  type ReportData,
  type ReportFilters as Filters,
  type ReportVariables,
  type RunReportData,
  type Workspace,
} from "./report.gql.ts";
import styles from "./ReportApp.module.css";
import { ReportFilters } from "./ReportFilters.tsx";
import { RunReportModal } from "./RunReportModal.tsx";

const ITEMS_PER_PAGE_OPTIONS = [20, 50, 100];
const DEFAULT_ITEMS_PER_PAGE = 50;
const POLL_INTERVAL_MS = 3000;

type ReportRow = {
  id: string;
  path: string;
  property: string;
  language: string;
  markups: string;
};

const isAccessDenied = (error: ApolloError): boolean =>
  error.graphQLErrors.some(e => /AccessDenied|permission/i.test(e.message ?? ""));

const formatDate = (iso: string | null, locale: string): string =>
  iso ? new Date(iso).toLocaleString(locale, { dateStyle: "medium", timeStyle: "short" }) : "";

const downloadCsv = (csv: string, siteKey: string) => {
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = `html-filtering-report-${siteKey}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
};

type RunStateProps = Readonly<{ report: Report | null | undefined; locale: string }>;

/** The workspace and the state of the last run. */
const RunState = ({ report, locale }: RunStateProps) => {
  const { t } = useTranslation(NAMESPACE);
  if (!report) {
    return (
        <Banner title={t("report.state.neverTitle")} variant="neutral" data-sel-role="report-state" data-sel-state="NEVER">
            {t("report.state.never")}
        </Banner>
    );
  }
  const values = {
    workspace: t(`report.workspace.${report.workspace}`),
    count: report.itemCount,
    max: report.maxItems,
    date: formatDate(report.finished ?? report.started, locale),
  };
  const variant = { RUNNING: "info", DONE: "neutral", LIMIT_REACHED: "warning", FAILED: "danger" } as const;
  return (
      <Banner
      title={t(`report.state.${report.status}Title`, values)}
      variant={variant[report.status]}
      data-sel-role="report-state"
      data-sel-state={report.status}
      data-sel-workspace={report.workspace}
      >
          {t(`report.state.${report.status}`, values)}
      </Banner>
  );
};

type ReportTableProps = Readonly<{
  report: Report | null | undefined;
  error: ApolloError | undefined;
  isLoading: boolean;
  isFiltered: boolean;
  currentPage: number;
  itemsPerPage: number;
  onPageChange: (page: number) => void;
  onItemsPerPageChange: (itemsPerPage: number) => void;
}>;

/** The rows of the last run that the filters keep, once the run ended. */
const ReportTable = ({ report, error, isLoading, isFiltered, currentPage, itemsPerPage, onPageChange, onItemsPerPageChange }: ReportTableProps) => {
  const { t } = useTranslation(NAMESPACE);
  const columns = useMemo<DataTableColumn<ReportRow>[]>(
    () => [
      { key: "path", label: t("report.columns.path"), isScrollable: true },
      { key: "property", label: t("report.columns.property"), width: "200px" },
      { key: "language", label: t("report.columns.language"), width: "120px" },
      {
        key: "markups",
        label: t("report.columns.markups"),
        width: "320px",
        // The markups come from the content, so React renders them as text.
        render: ({ value }) => <Typography className={styles.monospace}>{value}</Typography>,
      },
    ],
    [t],
  );
  const offset = (currentPage - 1) * itemsPerPage;
  const rows = useMemo<ReportRow[]>(
    () =>
      (report?.items.nodes ?? []).map((item, index) => ({
        id: String(offset + index),
        path: item.path,
        property: item.property,
        language: item.language ?? "",
        markups: item.markups.join(" "),
      })),
    [report, offset],
  );

  if (error) {
    return (
        <Banner title={t("report.errorTitle")} variant="danger" role="alert" data-sel-role="report-error">
            {isAccessDenied(error) ? t("report.accessDenied") : t("report.error")}
        </Banner>
    );
  }
  if (!report || report.status === "RUNNING") {
    return null;
  }
  if (rows.length === 0) {
    return isFiltered
      ? <EmptyData message={t("report.filters.noMatch")} data-sel-role="report-no-match"/>
      : <EmptyData message={t("report.empty")} data-sel-role="report-empty"/>;
  }
  return (
      <Paper hasPadding={false} className={styles.tableWrapper} aria-busy={isLoading} data-sel-role="report-table">
          <DataTable
        enablePagination
        primaryKey="id"
        data={rows}
        columns={columns}
        currentPage={currentPage}
        itemsPerPage={itemsPerPage}
        itemsPerPageOptions={ITEMS_PER_PAGE_OPTIONS}
        totalItems={report.items.totalCount}
        paginationProps={{ className: styles.stickyPagination }}
        i18n={{
          itemsPerPage: t("report.pagination.itemsPerPage"),
          of: t("report.pagination.of"),
        }}
        onPageChange={onPageChange}
        onItemsPerPageChange={onItemsPerPageChange}
      />
      </Paper>
  );
};

type ReportPageProps = Readonly<{ siteKey: string; language: string }>;

const ReportPage = ({ siteKey, language }: ReportPageProps) => {
  const { t } = useTranslation(NAMESPACE);
  const client = useApolloClient(globalThis.jahia?.apolloClient);
  const uiLanguage = globalThis.contextJsParameters?.uilang ?? "en";
  const sitePath = `/sites/${siteKey}`;
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_ITEMS_PER_PAGE);
  const [filters, setFilters] = useState<Filters>({});
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isAlreadyRunning, setIsAlreadyRunning] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  const { data, previousData, loading, error, refetch, startPolling, stopPolling } = useQuery<ReportData, ReportVariables>(REPORT_QUERY, {
    variables: {
      siteKey,
      ...filters,
      offset: (currentPage - 1) * itemsPerPage,
      limit: itemsPerPage,
    },
    fetchPolicy: "network-only",
    notifyOnNetworkStatusChange: true,
    client,
    skip: !siteKey,
  });

  const [runReport, { loading: isStarting }] = useMutation<RunReportData>(RUN_REPORT_MUTATION, { client });

  const shown = data ?? (loading ? previousData : undefined);
  const report = shown?.htmlFiltering?.report;
  const isRunning = report?.status === "RUNNING";

  // The page follows a run until it ends.
  useEffect(() => {
    if (isRunning) {
      startPolling(POLL_INTERVAL_MS);
    } else {
      stopPolling();
    }
    return stopPolling;
  }, [isRunning, startPolling, stopPolling]);

  const refresh = () => {
    refetch().catch(() => undefined);
  };

  const setFilter = (id: FilterId, value: string) => {
    setFilters(old => ({ ...old, [id]: value || undefined }));
    setCurrentPage(1);
  };

  const start = (workspace: Workspace) => {
    setIsModalOpen(false);
    runReport({ variables: { siteKey, workspace } })
      .then(result => {
        setIsAlreadyRunning(result.data?.htmlFiltering?.runReport === false);
        setCurrentPage(1);
        refresh();
      })
      .catch(() => undefined);
  };

  const exportCsv = () => {
    setIsExporting(true);
    client
      .query<ReportCsvData>({ query: REPORT_CSV_QUERY, variables: { siteKey, ...filters }, fetchPolicy: "network-only" })
      .then(result => {
        const csv = result.data?.htmlFiltering?.report?.csv;
        if (csv) {
          downloadCsv(csv, siteKey);
        }
      })
      .catch(() => undefined)
      .finally(() => setIsExporting(false));
  };

  const totalItems = report?.items.totalCount ?? 0;

  return (
      <LayoutContent
      hasPadding
      className={styles.layout}
      isLoading={!error && !shown && loading}
      data-sel-role="html-filtering-report"
      header={
          <Header
          title={t("report.title", { siteName: siteKey })}
          mainActions={[
              <Button
              key="run"
              variant="default"
              color="accent"
              size="big"
              icon={<ReportIcon/>}
              label={t("report.runReport")}
              isDisabled={isRunning || isStarting}
              data-sel-role="report-run"
              onClick={() => setIsModalOpen(true)}
            />,
          ]}
          toolbarLeft={
              <>
                  <Button
                variant="ghost"
                icon={<Download/>}
                label={t("report.exportCsv")}
                isDisabled={!report || isRunning || totalItems === 0 || isExporting}
                data-sel-role="report-export"
                onClick={exportCsv}
              />
                  <Button
                variant="ghost"
                icon={<Reload/>}
                label={t("report.refresh")}
                isDisabled={loading}
                data-sel-role="report-refresh"
                onClick={refresh}
              />
              </>
          }
        />
      }
      >
          <div className={styles.page}>
              {isAlreadyRunning && (
              <Banner title={t("report.alreadyRunningTitle")} variant="warning" role="alert" data-sel-role="report-already-running">
                  {t("report.alreadyRunning")}
              </Banner>
          )}
              <RunState report={report} locale={uiLanguage}/>
              {report && !isRunning && (
              <ReportFilters filters={filters} siteKey={siteKey} sitePath={sitePath} language={language} onChange={setFilter}/>
          )}
              {loading && !report ? (
                  <Loader size="big"/>
          ) : (
              <ReportTable
              report={report}
              error={error}
              isLoading={loading}
              isFiltered={Boolean(filters.path || filters.markup)}
              currentPage={currentPage}
              itemsPerPage={itemsPerPage}
              onPageChange={setCurrentPage}
              onItemsPerPageChange={value => {
                setItemsPerPage(value);
                setCurrentPage(1);
              }}
            />
          )}
          </div>
          <RunReportModal isOpen={isModalOpen} onRun={start} onClose={() => setIsModalOpen(false)}/>
      </LayoutContent>
  );
};

const ReportApp = ({ siteKey, language }: ReportPageProps) => {
  // The bundled moonstone tokens apply while this attribute is set, as vite.config.ts states.
  useEffect(() => {
    const root = document.documentElement;
    root.setAttribute(MOONSTONE_SCOPE_ATTRIBUTE, "");
    return () => root.removeAttribute(MOONSTONE_SCOPE_ATTRIBUTE);
  }, []);

  return (
      <I18nextProvider i18n={i18n} defaultNS={NAMESPACE}>
          <ReportPage siteKey={siteKey} language={language}/>
      </I18nextProvider>
  );
};

export default ReportApp;
