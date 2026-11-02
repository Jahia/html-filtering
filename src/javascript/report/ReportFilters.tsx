import { Button, FolderOpen, Input, Typography } from "@jahia/moonstone";
import { useEffect, useState, type ReactElement } from "react";
import { useTranslation } from "react-i18next";
import { NAMESPACE } from "../i18n.ts";
import { FilterChip, type ChipState, type FilterId } from "./FilterChip.tsx";
import type { ReportFilters as Filters } from "./report.gql.ts";
import styles from "./ReportApp.module.css";

type SetFilter = (id: FilterId, value: string) => void;

type TypedEditorProps = Readonly<{ hint: string; picker?: ReactElement | false; children: ReactElement }>;

/** The editor of a filter that takes a typed value or a picker choice. */
const TypedEditor = ({ hint, picker, children }: TypedEditorProps) => (
    <div className={`${styles.editorBody} ${styles.typedEditor}`}>
        {children}
        <Typography variant="caption" className={styles.secondary}>
            {hint}
        </Typography>
        {picker}
    </div>
);

type TextFilterProps = ChipState & Readonly<{
  id: FilterId;
  value: string;
  placeholder: string;
  hint: string;
  onChange: SetFilter;
  picker?: (closeEditor: () => void) => ReactElement | false;
}>;

const TextFilter = ({ id, value, placeholder, hint, onChange, picker, onClose, ...chip }: TextFilterProps) => {
  const { t } = useTranslation(NAMESPACE);
  const [draft, setDraft] = useState(value);
  useEffect(() => setDraft(value), [value]);
  const name = t(`report.columns.${id}`);

  const commit = (text: string) => {
    if (text.trim() !== value) {
      onChange(id, text.trim());
    }
  };

  return (
      <FilterChip
      {...chip}
      id={id}
      name={name}
      value={value}
      onClose={() => {
        commit(draft);
        onClose();
      }}
      onClear={() => onChange(id, "")}
      >
          {closeEditor => (
              <TypedEditor hint={hint} picker={picker?.(closeEditor)}>
                  <Input
            value={draft}
            placeholder={placeholder}
            aria-label={name}
            data-sel-role={`filter-${id}`}
            onChange={event => setDraft(event.target.value)}
            onKeyUp={event => {
              if (event.key === "Enter") {
                commit(draft);
              }
            }}
            onBlur={() => commit(draft)}
            onClear={() => {
              setDraft("");
              commit("");
            }}
          />
              </TypedEditor>
      )}
      </FilterChip>
  );
};

type ReportFiltersProps = Readonly<{
  filters: Filters;
  siteKey: string;
  sitePath: string;
  language: string;
  onChange: SetFilter;
}>;

export const ReportFilters = ({ filters, siteKey, sitePath, language, onChange }: ReportFiltersProps) => {
  const { t } = useTranslation(NAMESPACE);
  const [openFilter, setOpenFilter] = useState<FilterId | null>(null);
  const openPicker = globalThis.CE_API?.openPicker;

  const chip = (id: FilterId): ChipState => ({
    isOpen: openFilter === id,
    onOpen: () => setOpenFilter(id),
    onClose: () => setOpenFilter(null),
  });

  // The picker dialog opens below the editor Menu, so the editor closes first.
  const pickPath = (closeEditor: () => void) => {
    closeEditor();
    openPicker?.({
      type: "editorial",
      site: siteKey,
      lang: language,
      setValue: ([node]) => node && onChange("path", node.path),
    });
  };

  return (
      <div className={styles.filters} data-sel-role="report-filters">
          <TextFilter
        {...chip("path")}
        id="path"
        value={filters.path ?? ""}
        placeholder={t("report.filters.pathPlaceholder", { example: `${sitePath}/home`, interpolation: { escapeValue: false } })}
        hint={t("report.filters.pathHint")}
        picker={closeEditor =>
          Boolean(openPicker) && (
          <Button
            variant="outlined"
            icon={<FolderOpen/>}
            label={t("report.filters.pickPath")}
            data-sel-role="filter-path-picker"
            onClick={() => pickPath(closeEditor)}
          />
          )}
        onChange={onChange}
      />
          <TextFilter
        {...chip("markup")}
        id="markup"
        value={filters.markup ?? ""}
        placeholder={t("report.filters.markupPlaceholder")}
        hint={t("report.filters.markupHint")}
        onChange={onChange}
      />
      </div>
  );
};
