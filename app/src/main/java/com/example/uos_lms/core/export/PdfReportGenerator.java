package com.example.uos_lms.core.export;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** Draws a simple tabular report (title + header row + data rows) onto A4-ish PDF pages using
 * the first-party android.graphics.pdf API - no external PDF library dependency needed for
 * this app's flat, unformatted report/transcript data (no merged cells, no images). */
public final class PdfReportGenerator {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;
    private static final float ROW_HEIGHT = 22f;

    private PdfReportGenerator() {
    }

    public static byte[] generate(String title, List<String> headers, List<List<String>> rows) {
        PdfDocument document = new PdfDocument();
        try {
            Paint titlePaint = new Paint();
            titlePaint.setTextSize(16f);
            titlePaint.setFakeBoldText(true);
            Paint headerPaint = new Paint();
            headerPaint.setTextSize(11f);
            headerPaint.setFakeBoldText(true);
            Paint cellPaint = new Paint();
            cellPaint.setTextSize(10f);

            float columnWidth = (PAGE_WIDTH - 2f * MARGIN) / Math.max(1, headers.size());
            int rowsPerPage = Math.max(1, (int) ((PAGE_HEIGHT - 2 * MARGIN - 60) / ROW_HEIGHT));

            int rowIndex = 0;
            int pageNumber = 1;
            do {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                float y = MARGIN;
                if (pageNumber == 1) {
                    canvas.drawText(title, MARGIN, y, titlePaint);
                    y += 30;
                }
                for (int col = 0; col < headers.size(); col++) {
                    canvas.drawText(headers.get(col), MARGIN + col * columnWidth, y, headerPaint);
                }
                y += ROW_HEIGHT;

                int rowsOnThisPage = 0;
                while (rowIndex < rows.size() && rowsOnThisPage < rowsPerPage) {
                    List<String> row = rows.get(rowIndex);
                    for (int col = 0; col < row.size() && col < headers.size(); col++) {
                        canvas.drawText(row.get(col), MARGIN + col * columnWidth, y, cellPaint);
                    }
                    y += ROW_HEIGHT;
                    rowIndex++;
                    rowsOnThisPage++;
                }

                document.finishPage(page);
                pageNumber++;
            } while (rowIndex < rows.size());

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                document.writeTo(output);
            } catch (IOException e) {
                throw new IllegalStateException("Could not generate PDF.", e);
            }
            return output.toByteArray();
        } finally {
            document.close();
        }
    }
}
