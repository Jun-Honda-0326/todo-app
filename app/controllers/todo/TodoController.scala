package controllers.todo

import java.time.LocalDate
import javax.inject._
import lib.model.Todo.TodoStatus
import lib.model.{Todo, Category}
import lib.persistence.default.{TodoRepository, CategoryRepository}
import model.TodoWithCategory
import model.{ViewValueTodo, ViewValueTodoAdd, ViewValueTodoEdit}
import model.component.ViewValueUser
import mvc.auth._
import ixias.play.api.auth.mvc.AuthExtensionMethods
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc._
import scala.concurrent._

case class TodoForm(
    title: String,
    categoryId: Long,
    content: String,
    state: Short,
    deadline: LocalDate
)

@Singleton
class TodoController @Inject()(
    val controllerComponents: ControllerComponents,
    val authProfile: UserAuthProfile
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport
    with AuthExtensionMethods {
  //新規追加機能用のフォームオブジェクト
  val todoForm: Form[TodoForm] = Form(
    mapping(
      "title" -> nonEmptyText,
      "categoryId" -> longNumber,
      "content" -> nonEmptyText,
      "state" -> shortNumber(min = 0, max = 255),
      "deadline" -> localDate
    )(TodoForm.apply)(TodoForm.unapply)
  )

  //Todo一覧表示
  def list() = Authenticated(authProfile).async {
    implicit request: Request[AnyContent] =>
      authProfile.loggedIn { user =>
        for {
          todosEmbed <- TodoRepository.all()
          categoriesEmbed <- CategoryRepository.all()
        } yield {
          val vv = ViewValueTodo(
            head = "Todo一覧",
            cssSrc = Seq("main.css"),
            jsSrc = Seq("main.js"),
            user = Some(ViewValueUser.from(user)),
            todos = todosEmbed.map(
              todos =>
                TodoWithCategory(
                  todos.id,
                  todos.v.categoryId,
                  todos.v.title,
                  todos.v.content,
                  todos.v.state,
                  todos.v.deadline,
                  todos.v.updatedAt,
                  categoriesEmbed
                    .find(_.id == todos.v.categoryId)
                    .map(_.v.name),
                  categoriesEmbed
                    .find(_.id == todos.v.categoryId)
                    .map(_.v.color)
                )
            )
          )
          Ok(views.html.todo.list(vv))
        }
      }
  }
  //あいまい検索
  def search(keyword: String) = Authenticated(authProfile) async {
    implicit request: Request[AnyContent] =>
      authProfile.loggedIn { user =>
        for {
          todosEmbed <- TodoRepository.search(keyword)
          categoriesEmbed <- CategoryRepository.all()
        } yield {
          val vv = ViewValueTodo(
            head = "検索結果",
            cssSrc = Seq("main.css"),
            jsSrc = Seq("main.js"),
            user = Some(ViewValueUser.from(user)),
            todos = todosEmbed.map(
              todos =>
                TodoWithCategory(
                  todos.id,
                  todos.v.categoryId,
                  todos.v.title,
                  todos.v.content,
                  todos.v.state,
                  todos.v.deadline,
                  todos.v.updatedAt,
                  categoriesEmbed
                    .find(_.id == todos.v.categoryId)
                    .map(_.v.name),
                  categoriesEmbed
                    .find(_.id == todos.v.categoryId)
                    .map(_.v.color)
                )
            )
          )
          Ok(views.html.todo.list(vv))
        }
      }
  }

  //stateごとの一覧表示
  def listOfState(state: Int) = Authenticated(authProfile) async {
    implicit request: Request[AnyContent] =>
      authProfile.loggedIn { user =>
        val stateCode: Short = state.toShort
        val todoStatus = TodoStatus.apply(stateCode)
        for {
          todosEmbed <- TodoRepository.filterByStatus(todoStatus)
          categoriesEmbed <- CategoryRepository.all()
        } yield {
          val vv = ViewValueTodo(
            head = "進捗ごとのTodo一覧",
            cssSrc = Seq("main.css"),
            jsSrc = Seq("main.js"),
            user = Some(ViewValueUser.from(user)),
            todos = todosEmbed.map(
              todos =>
                TodoWithCategory(
                  todos.id,
                  todos.v.categoryId,
                  todos.v.title,
                  todos.v.content,
                  todos.v.state,
                  todos.v.deadline,
                  todos.v.updatedAt,
                  categoriesEmbed
                    .find(_.id == todos.v.categoryId)
                    .map(_.v.name),
                  categoriesEmbed
                    .find(_.id == todos.v.categoryId)
                    .map(_.v.color)
                )
            )
          )
          Ok(views.html.todo.list(vv))
        }
      }
  }
  //カテゴリーごとのTodo
  def todoCategory(id: Long) = Authenticated(authProfile) async {
    implicit request: Request[AnyContent] =>
      authProfile.loggedIn { user =>
        val categoryId = Category.Id(id)
        for {
          todosEmbed <- TodoRepository.filterByCategory(categoryId)
          categoriesEmbed <- CategoryRepository.all()
        } yield {
          val vv = ViewValueTodo(
            head = "カテゴリーごとのTodo",
            cssSrc = Seq("main.css"),
            jsSrc = Seq("main.js"),
            user = Some(ViewValueUser.from(user)),
            todos = todosEmbed.map(
              todos =>
                TodoWithCategory(
                  todos.id,
                  todos.v.categoryId,
                  todos.v.title,
                  todos.v.content,
                  todos.v.state,
                  todos.v.deadline,
                  todos.v.updatedAt,
                  categoriesEmbed
                    .find(_.id == todos.v.categoryId)
                    .map(_.v.name),
                  categoriesEmbed
                    .find(_.id == todos.v.categoryId)
                    .map(_.v.color)
                )
            )
          )
          Ok(views.html.todo.list(vv))
        }
      }
  }

  //登録画面の表示用
  def register() = Authenticated(authProfile) async {
    implicit request: Request[AnyContent] =>
      authProfile.loggedIn { user =>
        for {
          categoriesEmbed <- CategoryRepository.all()
        } yield {
          val vv = ViewValueTodoAdd(
            head = "Todo追加",
            cssSrc = Seq("main.css"),
            jsSrc = Seq("main.js"),
            user = Some(ViewValueUser.from(user)),
            todoForm = todoForm,
            categories = categoriesEmbed.map(
              category => (category.id.toString -> category.v.name)
            )
          )
          Ok(views.html.todo.add(vv))
        }
      }
  }

  //登録処理
  def add(): Action[AnyContent] = Authenticated(authProfile) async {
    implicit request: Request[AnyContent] =>
      authProfile.loggedIn { user =>
        todoForm
          .bindFromRequest()
          .fold(
            (errorForm: Form[TodoForm]) => {
              for {
                categoriesEmbed <- CategoryRepository.all()
              } yield {
                val vv = ViewValueTodoAdd(
                  head = "進捗ごとのTodo一覧",
                  cssSrc = Seq("main.css"),
                  jsSrc = Seq("main.js"),
                  user = Some(ViewValueUser.from(user)),
                  todoForm = errorForm,
                  categories = categoriesEmbed
                    .map(category => (category.id.toString -> category.v.name))
                )
                BadRequest(views.html.todo.add(vv))
              }
            },
            (todoForm: TodoForm) => {
              val todoWithNoId = new Todo(
                id = None,
                categoryId = Category.Id(todoForm.categoryId),
                title = todoForm.title,
                content = todoForm.content,
                state = TodoStatus.apply(todoForm.state),
                deadline = todoForm.deadline
              ).toWithNoId
              for {
                _ <- TodoRepository.add(todoWithNoId)
              } yield {
                Redirect(routes.TodoController.list())
                  .flashing("success" -> "Todoを追加しました!!")
              }
            }
          )
      }
  }

  //編集画面表示用
  def edit(id: Long): Action[AnyContent] = Authenticated(authProfile) async {
    implicit request: Request[AnyContent] =>
      authProfile.loggedIn { user =>
        val todoId = Todo.Id(id)
        for {
          todoEmbed <- TodoRepository.get(todoId)
          categoriesEmbed <- CategoryRepository.all()
        } yield {
          todoEmbed match {
            case Some(todoEmbed) =>
              val vv = ViewValueTodoEdit(
                id = id,
                head = "Todo編集",
                cssSrc = Seq("main.css"),
                jsSrc = Seq("main.js"),
                user = Some(ViewValueUser.from(user)),
                categories = categoriesEmbed.map(
                  category => (category.id.toString -> category.v.name)
                ),
                todoForm = todoForm.fill(
                  TodoForm(
                    todoEmbed.v.title,
                    todoEmbed.v.categoryId,
                    todoEmbed.v.content,
                    todoEmbed.v.state.code,
                    todoEmbed.v.deadline
                  )
                )
              )
              Ok(views.html.todo.edit(vv))
            //idが見つからない場合はトップページにリダイレクト
            case None =>
              Redirect(routes.TodoController.list())
          }
        }
      }
  }

  //更新処理
  def update(id: Long): Action[AnyContent] = Authenticated(authProfile) async {
    implicit request: Request[AnyContent] =>
    authProfile.loggedIn { user =>
      todoForm
        .bindFromRequest()
        .fold(
          //処理が失敗した場合
          (errorForm: Form[TodoForm]) => {
            for {
              categoriesEmbed <- CategoryRepository.all()
            } yield {
              val vv = ViewValueTodoAdd(
                head = "Todo編集",
                cssSrc = Seq("main.css"),
                jsSrc = Seq("main.js"),
                user = Some(ViewValueUser.from(user)),
                todoForm = errorForm,
                categories = categoriesEmbed
                  .map(category => (category.id.toString -> category.v.name))
              )
              BadRequest(views.html.todo.add(vv))
            }
          },
          //処理が成功した場合
          (todoForm: TodoForm) => {
            //取得したidを基にTodo[EntityEmbeddedId]をインスタンスを生成
            val todoEmbededId = new Todo(
              id = Some(Todo.Id(id)),
              categoryId = Category.Id(todoForm.categoryId),
              title = todoForm.title,
              content = todoForm.content,
              state = TodoStatus.apply(todoForm.state),
              deadline = todoForm.deadline
            ).toEmbeddedId //EmbededId型に変換
            for {
              todoUpdate <- TodoRepository.update(todoEmbededId)
            } yield {
              todoUpdate match {
                case None =>
                  Redirect(routes.TodoController.edit(id)) //更新が失敗した場合元のページにリダイレクト
                case Some(_) =>
                  Redirect(routes.TodoController.list())
                    .flashing("success" -> "Todoを更新しました!!")
                //更新できたらトップページにリダイレクト
              }
            }
          }
        )
    }
  }

  //削除処理
  def delete(id: Long): Action[AnyContent] = Action async {
    implicit request: Request[AnyContent] =>
      val todoId = Todo.Id(id)
      for {
        todoDelete <- TodoRepository.remove(todoId)
      } yield {
        todoDelete match {
          case _ =>
            Redirect(routes.TodoController.list())
              .flashing("success" -> "Todoを削除しました")
        }
      }
  }

}
