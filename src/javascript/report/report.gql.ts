import { gql } from "@apollo/client";

export type Workspace = "EDIT" | "LIVE";

export type ReportStatus = "RUNNING" | "DONE" | "LIMIT_REACHED" | "FAILED";

export type ReportItem = {
  path: string;
  property: string;
  language: string | null;
  markups: string[];
};

export type Report = {
  status: ReportStatus;
  workspace: Workspace;
  started: string;
  finished: string | null;
  maxItems: number;
  itemCount: number;
  platformBusy: boolean;
  items: {
    totalCount: number;
    nodes: ReportItem[];
  };
};

export type ReportFilters = {
  path?: string;
  markup?: string;
};

export type ReportData = {
  htmlFiltering: {
    report: Report | null;
  } | null;
};

export type ReportVariables = ReportFilters & {
  siteKey: string;
  offset: number;
  limit: number;
};

export const REPORT_QUERY = gql`
  query HtmlFilteringReport($siteKey: String!, $path: String, $markup: String, $offset: Int, $limit: Int) {
    htmlFiltering {
      report(siteKey: $siteKey) {
        status
        workspace
        started
        finished
        maxItems
        itemCount
        platformBusy
        items(path: $path, markup: $markup, offset: $offset, limit: $limit) {
          totalCount
          nodes {
            path
            property
            language
            markups
          }
        }
      }
    }
  }
`;

export type ReportCsvData = {
  htmlFiltering: {
    report: { csv: string } | null;
  } | null;
};

export const REPORT_CSV_QUERY = gql`
  query HtmlFilteringReportCsv($siteKey: String!, $path: String, $markup: String) {
    htmlFiltering {
      report(siteKey: $siteKey) {
        csv(path: $path, markup: $markup)
      }
    }
  }
`;

export type RunReportData = {
  htmlFiltering: {
    runReport: boolean;
  } | null;
};

export const RUN_REPORT_MUTATION = gql`
  mutation HtmlFilteringRunReport($siteKey: String!, $workspace: Workspace) {
    htmlFiltering {
      runReport(siteKey: $siteKey, workspace: $workspace)
    }
  }
`;
