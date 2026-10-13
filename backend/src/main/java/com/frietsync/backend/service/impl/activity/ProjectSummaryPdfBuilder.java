package com.frietsync.backend.service.impl.activity;

import com.frietsync.backend.dto.activity.DelayedIssueResponse;
import com.frietsync.backend.dto.activity.ProjectSummaryResponse;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class ProjectSummaryPdfBuilder {

    public byte[] build(ProjectSummaryResponse s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addTitle(document, "Project Summary");
            addLine(document, "Project ID: " + s.getProjectId());

            addHeading(document, "Issues");
            addLine(document, "Total: " + s.getTotalIssues());
            addLine(document, "Open: " + s.getOpenIssues());
            addLine(document, "In progress: " + s.getInProgressIssues());
            addLine(document, "Resolved: " + s.getResolvedIssues());
            addLine(document, "Reopened: " + s.getReopenedIssues());
            addLine(document, "Closed: " + s.getClosedIssues());
            addLine(document, "Completion: " + s.getCompletionPercent() + "%");

            addHeading(document, "Estimated hours");
            addLine(document, "Total estimated: " + s.getEstimatedHoursTotal());
            addLine(document, "Completed: " + s.getEstimatedHoursCompleted());
            addLine(document, "Remaining: " + s.getEstimatedHoursRemaining());
            if (s.getAverageDaysToResolve() == null) {
                addLine(document, "Average time to resolve: n/a");
            } else {
                addLine(document, "Average time to resolve: " + s.getAverageDaysToResolve() + " days");
            }

            addHeading(document, "Delays");
            addLine(document, "Overdue issues: " + s.getOverdueIssues());

            if (!s.getDelayedIssues().isEmpty()) {
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.addCell("#");
                table.addCell("Title");
                table.addCell("Status");
                table.addCell("Due date");
                table.addCell("Days late");
                for (DelayedIssueResponse d : s.getDelayedIssues()) {
                    table.addCell(String.valueOf(d.getIssueNumber()));
                    table.addCell(d.getTitle());
                    table.addCell(String.valueOf(d.getStatus()));
                    table.addCell(String.valueOf(d.getDueDate()));
                    table.addCell(String.valueOf(d.getDaysLate()));
                }
                document.add(table);
            }

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not create the PDF");
        }

        return out.toByteArray();
    }

    private void addTitle(Document document, String text) throws DocumentException {
        Font font = new Font(Font.HELVETICA, 20, Font.BOLD);
        document.add(new Paragraph(text, font));
    }

    private void addHeading(Document document, String text) throws DocumentException {
        Font font = new Font(Font.HELVETICA, 14, Font.BOLD);
        document.add(new Paragraph(" "));
        document.add(new Paragraph(text, font));
    }

    private void addLine(Document document, String text) throws DocumentException {
        document.add(new Paragraph(text));
    }
}