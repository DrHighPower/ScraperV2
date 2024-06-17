package scraper.HighPower.application;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;
import scraper.HighPower.domain.Rental;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * The ExcelHandler class provides functionalities for creating and managing Excel sheets
 * with rental information using the Apache POI library. It supports creating structured tables
 * with headers, adding rental details, and calculating budget-related information.
 *
 * <p>This class includes methods for:</p>
 * <ul>
 *   <li>Creating Excel sheets with predefined headers and rental data.</li>
 *   <li>Formatting cells with various styles for better readability.</li>
 *   <li>Calculating and displaying budgetary information based on user input and rental details.</li>
 *   <li>Handling fuel cost calculations based on distance and fuel consumption.</li>
 * </ul>
 *
 * <p>The class uses several constants to define headers, default styles, and calculation formulas
 * for the Excel sheets.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * List<Rental> rentals = Arrays.asList(
 *     new Rental("Rental 1", 100, 50, "http://example.com/1"),
 *     new Rental("Rental 2", 200, 75, "http://example.com/2")
 * );
 * ExcelHandler.createSheet(rentals, "RentalData");
 * }</pre>
 *
 * <p>The created Excel sheet will include the rental information, calculated budget, and other
 * relevant details as specified in the constants and methods of this class.</p>
 *
 * @see Rental
 * @see ApplicationSession
 * @see org.apache.poi.ss.usermodel.Workbook
 * @see org.apache.poi.xssf.usermodel.XSSFWorkbook
 */
public class ExcelHandler {
    private static final String OUTPUT = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "output" + File.separator;
    private static final String[] HEADER = {
            "Number", "Name", "Distance in km", "Price per night", "Total price",
            "Leftover", "Leftover with gasoline", "Leftover with diesel", "Budget needed per person", "URL"
    };
    private static final int[] HEADER_WIDTH = {
            3156, 9019, 3857, 3480, 3480,
            3480, 3770, 3770, 4350, 2320
    };

    private static final int MAX_PEOPLE = ApplicationSession.getPeopleQuantity();
    private static final int MAX_PRICE = ApplicationSession.getMaximumPrice();

    // Get the quantity of nights depending on the search method
    private static final int NIGHT_QUANTITY = ApplicationSession.getFlexibility() ?
            ApplicationSession.getNightQuantity() :
            (int) DAYS.between(ApplicationSession.getStartDate(), ApplicationSession.getEndDate()) - 1;

    private static final int CELL_NAME_SIZE = 6583;
    private static final int CELL_VALUE_SIZE = 2320;
    private static final double GAS_PRICE = 1.67; // In EUR
    private static final double DIESEL_PRICE = 1.73; // In EUR
    private static final double FUEL_CONSUMPTION = 6.5; // Per 100km

    /**
     * Converts an integer to the corresponding Excel column name.
     *
     * @param columnNumber The column number to convert.
     * @return The Excel column name.
     */
    private static String convertToTitle(int columnNumber) {
        StringBuilder columnName = new StringBuilder();

        while (columnNumber > 0) {
            // Find remainder with 26
            int rem = columnNumber % 26;

            // If remainder is 0, then a 'Z' must be there in output
            if (rem == 0) {
                columnName.append('Z');
                columnNumber = (columnNumber / 26) - 1;
            } else { // If remainder is non-zero
                columnName.append((char) ((rem - 1) + 'A'));
                columnNumber = columnNumber / 26;
            }
        }

        // Reverse the string and return
        return columnName.reverse().toString();
    }

    /**
     * Searches for a string in an array and returns its 1-based index.
     *
     * @param arr The array to search.
     * @param str The string to search for.
     * @return The 1-based index of the string, or -1 if not found.
     */
    private static int arraySearch(String[] arr, String str) {
        if (arr == null || str == null) return -1;

        for (int i = 0; i < arr.length; i++) {
            if (str.equals(arr[i])) {
                return i + 1;  // Return 1-based index
            }
        }

        return -1;  // Return -1 if the string is not found
    }

    /**
     * Creates a cell style for the header cells.
     *
     * @param workbook The workbook to create the style for.
     * @return The header cell style.
     */
    private static CellStyle createHeaderCellStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    /**
     * Creates a cell style for the distance cells.
     *
     * @param workbook The workbook to create the style for.
     * @return The distance cell style.
     */
    private static CellStyle createDistanceCellStyle(XSSFWorkbook workbook) {
        DataFormat format = workbook.createDataFormat();
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(format.getFormat("0.0\" km\""));
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Creates a cell style for the price cells.
     *
     * @param workbook The workbook to create the style for.
     * @return The price cell style.
     */
    private static CellStyle createPriceCellStyle(XSSFWorkbook workbook) {
        DataFormat format = workbook.createDataFormat();
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(format.getFormat("# ##0.00 €"));
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Creates a cell style for number cells.
     *
     * @param workbook The workbook to create the style for.
     * @return The number cell style.
     */
    private static CellStyle createNumberCellStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Creates a cell style for URL cells.
     *
     * @param workbook The workbook to create the style for.
     * @return The URL cell style.
     */
    private static CellStyle createURLCellStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.FILL);
        return style;
    }

    /**
     * Creates the header row and table columns for the specified sheet.
     *
     * @param sheet    The sheet to create the header for.
     * @param cttable  The table to add columns to.
     * @param workbook The workbook to create the styles for.
     */
    private static void createHeader(Sheet sheet, CTTable cttable, XSSFWorkbook workbook) {
        CTTableColumns columns = cttable.addNewTableColumns();
        columns.setCount(HEADER.length);

        // Initializes the header row
        XSSFRow row = (XSSFRow) sheet.createRow(0);
        row.setHeight((short) 867); // 72px

        // Get the header style
        CellStyle headerStyle = createHeaderCellStyle(workbook);

        // Goes through all the columns
        for (int i = 0; i < HEADER.length; i++) {
            CTTableColumn column = columns.addNewTableColumn();
            column.setId(i + 1);

            column.setName(HEADER[i]);
            sheet.setColumnWidth(i, HEADER_WIDTH[i]);

            // Names the header
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(HEADER[i]);

            // Style the header text
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Adds the rental information to the specified sheet.
     *
     * @param rentals  The list of rentals to add.
     * @param sheet    The sheet to add the rentals to.
     * @param workbook The workbook to create the styles for.
     */
    private static void addTableInfo(List<Rental> rentals, Sheet sheet, XSSFWorkbook workbook) {
        // Column with the variables
        String variableColumn = convertToTitle(HEADER.length + 3);

        CellStyle price = createPriceCellStyle(workbook);

        // Go through all the rentals
        for (int i = 0; i < rentals.size(); i++) {
            XSSFRow row = (XSSFRow) sheet.createRow(i + 1); // Creates the row
            Rental rental = rentals.get(i); // Get current rental

            // Creates the cell for the rental number
            XSSFCell numberCell = row.createCell(0);
            numberCell.setCellStyle(createNumberCellStyle(workbook));
            numberCell.setCellValue(i + 1);

            // Creates the cell for the rental name
            row.createCell(1).setCellValue(rental.getName());

            // Creates the cell for the rental distance
            XSSFCell distanceCell = row.createCell(2);
            distanceCell.setCellStyle(createDistanceCellStyle(workbook));
            distanceCell.setCellValue(rental.getDistance());

            // Creates the cell for the rental price per night
            XSSFCell pricePerNightCell = row.createCell(3);
            pricePerNightCell.setCellStyle(price);
            pricePerNightCell.setCellValue(rental.getPricePerNight());

            // Creates the total price
            XSSFCell totalPrice = row.createCell(4);
            totalPrice.setCellStyle(price);
            totalPrice.setCellFormula(convertToTitle(arraySearch(HEADER, "Price per night")) + (i + 2) + "*" + variableColumn + "6");

            // Creates the leftover
            XSSFCell leftover = row.createCell(5);
            leftover.setCellStyle(price);
            leftover.setCellFormula(variableColumn + "4-" + convertToTitle(arraySearch(HEADER, "Total price")) + (i + 2));

            // Creates the gas leftover
            XSSFCell leftoverGas = row.createCell(6);
            leftoverGas.setCellStyle(price);
            leftoverGas.setCellFormula(
                    variableColumn + "4-" + "(" + convertToTitle(arraySearch(HEADER, "Total price")) + (i + 2) +
                            "+((" + variableColumn + "11*" + convertToTitle(arraySearch(HEADER, "Distance in km")) + (i + 2) +
                            ")/100)*" + variableColumn + "9" + ")"
            );

            // Creates the diesel leftover
            XSSFCell leftoverDiesel = row.createCell(7);
            leftoverDiesel.setCellStyle(price);
            leftoverDiesel.setCellFormula(
                    variableColumn + "4-" + "(" + convertToTitle(arraySearch(HEADER, "Total price")) + (i + 2) +
                            "+((" + variableColumn + "11*" + convertToTitle(arraySearch(HEADER, "Distance in km")) + (i + 2) +
                            ")/100)*" + variableColumn + "10" + ")"
            );

            // Creates the budget per person
            XSSFCell budgetPerPerson = row.createCell(8);
            budgetPerPerson.setCellStyle(price);
            budgetPerPerson.setCellFormula(convertToTitle(arraySearch(HEADER, "Total price")) + (i + 2) + "/" + variableColumn + "3");

            // Creates the URL
            XSSFCell URL = row.createCell(9);
            URL.setCellValue(rental.getUrl());
            URL.setCellStyle(createURLCellStyle(workbook));
        }
    }

    /**
     * Adds a thin border around a cell.
     *
     * @param style The cell style to add the border to.
     */
    private static void border(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    /**
     * Creates a row for variable information.
     *
     * @param sheet    The sheet to create the row in.
     * @param workbook The workbook to create the styles for.
     * @param rowPos   The position of the row.
     * @param text     The text for the row.
     * @param value    The value for the row.
     * @return The created cell for the value.
     */
    private static XSSFCell createVariableRow(Sheet sheet, XSSFWorkbook workbook, int rowPos, String text, double value) {
        XSSFRow row = (XSSFRow) sheet.getRow(rowPos);
        if (row == null) row = (XSSFRow) sheet.createRow(rowPos);

        // Style the text
        CellStyle cellTextStyle = workbook.createCellStyle();
        cellTextStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        cellTextStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        border(cellTextStyle);

        XSSFCell cellText = row.createCell(HEADER.length + 1);
        XSSFCell cellValue = row.createCell(HEADER.length + 2);

        cellText.setCellValue(text);
        cellValue.setCellValue(value);

        cellText.setCellStyle(cellTextStyle);
        return cellValue;
    }

    /**
     * Adds variable information to the specified sheet.
     *
     * @param sheet    The sheet to add the variable information to.
     * @param workbook The workbook to create the styles for.
     */
    private static void addVariableInfo(Sheet sheet, XSSFWorkbook workbook) {
        DataFormat format = workbook.createDataFormat();
        sheet.setColumnWidth(HEADER.length + 1, CELL_NAME_SIZE);
        sheet.setColumnWidth(HEADER.length + 2, CELL_VALUE_SIZE);

        // Get the styles for the cells
        CellStyle price = createPriceCellStyle(workbook);
        CellStyle number = createNumberCellStyle(workbook);
        border(price);
        border(number);

        // Column with the variables
        String column = convertToTitle(HEADER.length + 3);

        // Creates the table for the budget per person
        createVariableRow(sheet, workbook, 1, "Budget per person", MAX_PRICE).setCellStyle(price);
        createVariableRow(sheet, workbook, 2, "Person quant.", MAX_PEOPLE).setCellStyle(number);

        // Creates the total
        XSSFCell total = createVariableRow(sheet, workbook, 3, "Total", 0);
        total.setCellStyle(price);
        total.setCellFormula(column + "2*" + column + "3");

        // Night quantity
        createVariableRow(sheet, workbook, 5, "Nights", NIGHT_QUANTITY).setCellStyle(number);
        XSSFCell budgetPerNight = createVariableRow(sheet, workbook, 6, "Budget per night", 0);
        budgetPerNight.setCellStyle(price);
        budgetPerNight.setCellFormula(column + "4/" + column + "6");

        // Creates the table for the gas prices
        createVariableRow(sheet, workbook, 8, "Gasoline price", GAS_PRICE).setCellStyle(price);
        createVariableRow(sheet, workbook, 9, "Diesel price", DIESEL_PRICE).setCellStyle(price);
        createVariableRow(sheet, workbook, 10, "Fuel consumption per 100 km", FUEL_CONSUMPTION).setCellStyle(number);
    }

    /**
     * Creates an Excel sheet with rental information.
     *
     * @param rentals   The list of rentals to add to the sheet.
     * @param sheetName The name of the sheet.
     */
    public static void createSheet(List<Rental> rentals, String sheetName) {
        new File(OUTPUT).mkdir();
        File fp = new File(OUTPUT + sheetName + ".xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(sheetName);  // Creates the sheet
            XSSFTable table = sheet.createTable(null); // Creates the table

            // Create the table layout
            CTTable cttable = table.getCTTable();
            cttable.setRef("A1:" + convertToTitle(HEADER.length) + (rentals.size() + 1)); // Size of the table

            // Style the table
            CTTableStyleInfo styleInfo = cttable.addNewTableStyleInfo();
            styleInfo.setName("TableStyleLight15"); // Colors used
            styleInfo.setShowColumnStripes(false);
            styleInfo.setShowRowStripes(true);

            // Creates the header row
            createHeader(sheet, cttable, workbook);

            // Adds the rentals info
            addTableInfo(rentals, sheet, workbook);

            // Creates the cells with variables
            addVariableInfo(sheet, workbook);

            // Write to the file
            FileOutputStream outputStream = new FileOutputStream(fp);
            workbook.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
