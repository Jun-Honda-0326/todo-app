package model

import lib.model.Category
import lib.model.Category.CategoryColor
import controllers.category.CategoryForm
import play.api.data.Form
import play.api.data.Form._

case class ViewValueCategory(
  head:     String,
  cssSrc:   Seq[String],
  jsSrc:    Seq[String],
  category: Seq[Category]
)extends ViewValueCommon

case class ViewValueCategoryForm(
  head:      String,
  cssSrc:    Seq[String],
  jsSrc:     Seq[String],
  category:  Form[CategoryForm]
)extends ViewValueCommon