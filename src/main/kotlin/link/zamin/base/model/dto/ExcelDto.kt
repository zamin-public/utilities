package link.zamin.base.model.dto

data class ExcelData(
    val sheets: List<SheetData>
)

data class SheetData(
    val rows: List<RowData>
)

data class RowData(
    val cells: List<String?>
)

data class ExcelUrl(
    val url: String,
)