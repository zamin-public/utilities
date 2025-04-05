package link.zamin.base.utility

import link.zamin.base.model.dto.ExcelData
import link.zamin.base.model.dto.RowData
import link.zamin.base.model.dto.SheetData
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.InputStream

object ExcelUtil {

    fun parseData(file: File): ExcelData {
        val workbook = XSSFWorkbook(file.inputStream())

        return parseData(workbook)
    }

    fun parseData(file: InputStream): ExcelData {
        val workbook = XSSFWorkbook(file)

        return parseData(workbook)
    }

    fun parseData(workbook: XSSFWorkbook): ExcelData {
        val sheets = mutableListOf<SheetData>()
        workbook.sheetIterator().forEach { sheet ->
            val rows = mutableListOf<RowData>()
            sheet.rowIterator().forEach { row ->
                val cells = mutableListOf<String?>()
                for (i in 0 until row.lastCellNum) {
                    val cell = row.getCell(i, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK)
                    cell?.cellType = CellType.STRING
                    cells.add(cell?.stringCellValue.toString().replace(".0", ""))
                }
                rows.add(RowData(cells))
            }
            sheets.add(SheetData(rows))
        }

        return ExcelData(sheets)
    }
}
