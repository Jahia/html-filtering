import { Button, Modal, ModalBody, ModalFooter, ModalHeader, RadioGroup, RadioItem } from "@jahia/moonstone";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { NAMESPACE } from "../i18n.ts";
import type { Workspace } from "./report.gql.ts";

type RunReportModalProps = Readonly<{
  isOpen: boolean;
  onRun: (workspace: Workspace) => void;
  onClose: () => void;
}>;

export const RunReportModal = ({ isOpen, onRun, onClose }: RunReportModalProps) => {
  const { t } = useTranslation(NAMESPACE);
  const [workspace, setWorkspace] = useState<Workspace>("EDIT");

  return (
      <Modal isOpen={isOpen} size="small" data-sel-role="report-run-modal" onOpenChange={open => !open && onClose()}>
          <>
              <ModalHeader title={t("report.run.title")}/>
              <ModalBody>
                  <RadioGroup name="workspace" value={workspace} onChange={(_event, value) => setWorkspace(value as Workspace)}>
                      <RadioItem
              id="report-workspace-edit"
              value="EDIT"
              label={t("report.workspace.EDIT")}
              description={t("report.run.editDescription")}
              data-sel-role="report-workspace-edit"
            />
                      <RadioItem
              id="report-workspace-live"
              value="LIVE"
              label={t("report.workspace.LIVE")}
              description={t("report.run.liveDescription")}
              data-sel-role="report-workspace-live"
            />
                  </RadioGroup>
              </ModalBody>
              <ModalFooter>
                  <Button variant="ghost" label={t("report.run.cancel")} data-sel-role="report-run-cancel" onClick={onClose}/>
                  <Button
            variant="default"
            color="accent"
            label={t("report.run.start")}
            data-sel-role="report-run-start"
            onClick={() => onRun(workspace)}
          />
              </ModalFooter>
          </>
      </Modal>
  );
};
