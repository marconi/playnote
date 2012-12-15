package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import scala.collection.JavaConversions._ // auto convertion of types from java to scala

import com.evernote.thrift.transport.THttpClient
import com.evernote.thrift.protocol.TBinaryProtocol
import com.evernote.edam.userstore.UserStore
import com.evernote.edam.notestore.NoteStore

import com.evernote.edam.`type`.Notebook


object Application extends Controller {

  val devToken: String = "<devtoken goes here>"
  var noteStore: NoteStore.Client = _
  val notebookForm = Form("name" -> nonEmptyText)

  def index = Action {
    Ok(views.html.index(notebookForm, getNotebooks()))
  }

  def newNotebook = Action { implicit request =>
    notebookForm.bindFromRequest.fold(
      errors => BadRequest("Something went wrong"),
      name => {
        println(name)
        val notebook: Notebook = new Notebook()
        notebook.setName(name)
        noteStore.createNotebook(devToken, notebook)
        Redirect(routes.Application.index)
      })
  }
  
  def deleteNotebook(guid: String) = Action {
    noteStore.expungeNotebook(devToken, guid)
    Redirect(routes.Application.index)
  }
  
  def getNotebooks(): List[Notebook] = {
    val userStoreUrl: String = "https://sandbox.evernote.com/edam/user"

    // get NoteStore URL
    val userStoreProt: TBinaryProtocol = new TBinaryProtocol(new THttpClient(userStoreUrl))
    val userStore: UserStore.Client = new UserStore.Client(userStoreProt, userStoreProt)
    val noteStoreUrl: String = userStore.getNoteStoreUrl(devToken)

    // setup NoteStore client
    val noteStoreTrans: THttpClient = new THttpClient(noteStoreUrl)
    noteStoreTrans.setCustomHeader("User-Agent", "PlayNote 0.1")
    val noteStoreProt: TBinaryProtocol = new TBinaryProtocol(noteStoreTrans)
    noteStore = new NoteStore.Client(noteStoreProt, noteStoreProt)

    // fetch notebooks and list it
    noteStore.listNotebooks(devToken).toList
  }

}
