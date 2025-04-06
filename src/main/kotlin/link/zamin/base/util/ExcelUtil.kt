package link.zamin.base.util

import link.zamin.base.model.dto.ExcelData
import link.zamin.base.model.dto.RowData
import link.zamin.base.model.dto.SheetData
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.InputStream

object ExcelUtil {

    fun parseData(file: File, replace: ParseType = ParseType.ORIGINAL): ExcelData {
        val workbook = XSSFWorkbook(file.inputStream())
        return parseData(workbook, replace)
    }

    fun parseData(file: InputStream, replace: ParseType = ParseType.ORIGINAL): ExcelData {
        val workbook = XSSFWorkbook(file)
        return parseData(workbook, replace)
    }

    fun parseData(workbook: XSSFWorkbook, replace: ParseType = ParseType.ORIGINAL): ExcelData {
        val sheets = workbook.map { sheet ->
            val rows = sheet.map { row ->
                val cells = (0 until row.lastCellNum).map { i ->
                    val cell = row.getCell(i, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK)
                    parseCell(cell, replace)
                }
                RowData(cells)
            }
            SheetData(rows)
        }

        return ExcelData(sheets)
    }

    private fun parseCell(cell: Cell?, replace: ParseType): String? {
        if (cell == null) return null

        return when (replace) {
            ParseType.ORIGINAL -> cell.toString()
            ParseType.REPLACE_POINT_ZERO -> when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue
                CellType.NUMERIC -> cell.numericCellValue.toString().removeSuffix(".0")
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> when (cell.cachedFormulaResultType) {
                    CellType.STRING -> cell.stringCellValue
                    CellType.NUMERIC -> cell.numericCellValue.toString().removeSuffix(".0")
                    CellType.BOOLEAN -> cell.booleanCellValue.toString()
                    else -> null
                }

                else -> null
            }
        }
    }

    enum class ParseType {
        REPLACE_POINT_ZERO,
        ORIGINAL
    }
}