package com.wellsfargo.creditdecision.authoring.excel.extractor;

import com.wellsfargo.creditdecision.authoring.excel.model.KnockoutPolicySheetRow;
import com.wellsfargo.creditdecision.authoring.excel.model.KnockoutSheetMetadata;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class KnockoutPolicyImportSmokeTest {

    // Update this path if your workbook is somewhere else.
    private static final String WORKBOOK_PATH =
            "src/test/resources/DHW_POS_WF_CreditApply_WIPX1.xlsx";

    private static final String SHEET_NAME = "Knockout Calcs & Policy";

    @Test
    void shouldExtractKnockoutMetadataAndPolicyRows() throws Exception {
        Path workbookPath = Paths.get(WORKBOOK_PATH);
        assertTrue(Files.exists(workbookPath),
                "Workbook not found at: " + workbookPath.toAbsolutePath());

        try (InputStream inputStream = Files.newInputStream(workbookPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            assertNotNull(sheet, "Sheet not found: " + SHEET_NAME);

            KnockoutSheetMetadataExtractor metadataExtractor = new KnockoutSheetMetadataExtractor();
            KnockoutPolicySheetExtractor policyExtractor = new KnockoutPolicySheetExtractor();

            KnockoutSheetMetadata metadata = metadataExtractor.extract(sheet);
            List<KnockoutPolicySheetRow> rows = policyExtractor.extractPolicyRows(sheet);

            assertNotNull(metadata, "Metadata must not be null");
            assertNotNull(rows, "Rows list must not be null");
            assertFalse(rows.isEmpty(), "Expected knockout policy rows but extractor returned none");

            printMetadata(metadata);
            printRows(rows);

            Optional<KnockoutPolicySheetRow> q18 = rows.stream()
                    .filter(row -> "Q18".equalsIgnoreCase(safe(row.getPolicyCode())))
                    .findFirst();

            Optional<KnockoutPolicySheetRow> d22 = rows.stream()
                    .filter(row -> "D22".equalsIgnoreCase(safe(row.getPolicyCode())))
                    .findFirst();

            assertTrue(q18.isPresent(), "Q18 row was not extracted");
            assertTrue(d22.isPresent(), "D22 row was not extracted");

            // Q18 assertions
            assertEquals("KNOCKOUT", safe(q18.get().getPolicyCategory()).toUpperCase(),
                    "Q18 policy category should be KNOCKOUT");
            assertFalse(q18.get().getInputParameters().isEmpty(),
                    "Q18 input parameters should not be empty");
            assertFalse(isBlank(q18.get().getFormulaExpression()),
                    "Q18 formula expression should not be blank");

            // D22 assertions
            assertEquals("KNOCKOUT", safe(d22.get().getPolicyCategory()).toUpperCase(),
                    "D22 policy category should be KNOCKOUT");
            assertFalse(d22.get().getInputParameters().isEmpty(),
                    "D22 input parameters should not be empty");
            assertFalse(isBlank(d22.get().getFormulaExpression()),
                    "D22 formula expression should not be blank");
        }
    }

    private void printMetadata(KnockoutSheetMetadata metadata) {
        System.out.println("===== KNOCKOUT METADATA =====");
        System.out.println("sheetName=" + safe(metadata.getSheetName()));
        System.out.println("outputPath=" + safe(metadata.getOutputPath()));
        System.out.println("primaryApplicantOnly=" + metadata.getPrimaryApplicantOnly());
        System.out.println("stopProcessingIfAnyPolicyTriggered=" + metadata.getStopProcessingIfAnyPolicyTriggered());
        System.out.println("rawInstructions=");
        System.out.println(safe(metadata.getRawInstructions()));
        System.out.println("================================");
    }

    private void printRows(List<KnockoutPolicySheetRow> rows) {
        System.out.println("===== EXTRACTED KNOCKOUT ROWS =====");
        for (KnockoutPolicySheetRow row : rows) {
            printRow(row);
        }
        System.out.println("===================================");
    }

    private void printRow(KnockoutPolicySheetRow row) {
        System.out.println("rowNum=" + row.getRowNumber());
        System.out.println("ruleId=" + safe(row.getRuleId()));
        System.out.println("ruleName=" + safe(row.getRuleName()));
        System.out.println("policyOrCalculation=" + safe(row.getPolicyOrCalculation()));
        System.out.println("inputParameters=" + row.getInputParameters());
        System.out.println("formulaExpression=" + safe(row.getFormulaExpression()));
        System.out.println("policyCode=" + safe(row.getPolicyCode()));
        System.out.println("policyCategory=" + safe(row.getPolicyCategory()));
        System.out.println("locationInBom=" + safe(row.getLocationInBom()));
        System.out.println("notes=" + safe(row.getNotes()));
        System.out.println("----------------------------------");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
