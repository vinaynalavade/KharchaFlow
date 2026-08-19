package com.vinaynalavade.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon_name")
    val iconName: String,

    @ColumnInfo(name = "color_hex")
    val colorHex: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false
) {
    fun toDomainModel(): Category {
        return Category(
            id = id,
            name = name,
            iconName = iconName,
            colorHex = colorHex,
            type = TransactionType.fromString(type),
            isDefault = isDefault
        )
    }

    companion object {
        fun fromDomainModel(category: Category): CategoryEntity {
            return CategoryEntity(
                id = category.id,
                name = category.name,
                iconName = category.iconName,
                colorHex = category.colorHex,
                type = category.type.name,
                isDefault = category.isDefault
            )
        }
    }
}
