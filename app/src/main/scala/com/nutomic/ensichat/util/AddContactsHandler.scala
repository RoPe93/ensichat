package com.nutomic.ensichat.util

import android.app.{Notification, NotificationManager, PendingIntent}
import android.content.{Context, Intent}
import android.util.Log
import android.widget.Toast
import com.nutomic.ensichat.R
import com.nutomic.ensichat.activities.ConfirmAddContactActivity
import com.nutomic.ensichat.protocol.ChatService.OnMessageReceivedListener
import com.nutomic.ensichat.protocol.messages.{RequestAddContact, Message, ResultAddContact}
import com.nutomic.ensichat.protocol.{Address, User}

/**
 * Handles [[RequestAddContact]] and [[ResultAddContact]] messages, adds new contacts.
 *
 * @param getUser Returns info about a given address.
 * @param localAddress Address of the local device.
 */
class AddContactsHandler(context: Context, getUser: (Address) => User, localAddress: Address)
  extends OnMessageReceivedListener {

  private val Tag = "AddContactsHandler"

  private val notificationIdAddContactGenerator = Stream.from(100).iterator

  private lazy val database = new Database(context)

  private var currentlyAdding = Map[Address, AddContactInfo]()

  private case class AddContactInfo(localConfirmed: Boolean, remoteConfirmed: Boolean)

  def onMessageReceived(msg: Message): Unit = {
    val remote =
      if (msg.Header.origin == localAddress)
        msg.Header.target
      else
        msg.Header.origin

    msg.Body match {
      case _: RequestAddContact =>
        Log.i(Tag, "Remote device " + remote + " wants to add us as a contact")
        currentlyAdding += (remote -> new AddContactInfo(false, false))

        // Don't show notification for requests coming from local device.
        if (msg.Header.origin == localAddress)
          return

        val intent = new Intent(context, classOf[ConfirmAddContactActivity])
        intent.putExtra(ConfirmAddContactActivity.ExtraContactAddress, msg.Header.origin.toString)
        val pi = PendingIntent.getActivity(context, 0, intent,
          PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = new Notification.Builder(context)
          .setContentTitle(context.getString(R.string.notification_friend_request, getUser(remote)))
          .setSmallIcon(R.drawable.ic_launcher)
          .setContentIntent(pi)
          .setAutoCancel(true)
          .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
        nm.notify(notificationIdAddContactGenerator.next(), notification)
      case res: ResultAddContact =>
        if (!currentlyAdding.contains(remote)) {
          Log.w(Tag, "ResultAddContact without previous RequestAddContact, ignoring")
          return
        }

        val newInfo =
        if (msg.Header.origin == localAddress)
          new AddContactInfo(res.accepted, currentlyAdding(remote).remoteConfirmed)
        else
          new AddContactInfo(currentlyAdding(remote).localConfirmed, res.accepted)
        currentlyAdding += (remote -> newInfo)

        if (res.accepted)
          addContactIfBothConfirmed(remote)
        else {
          Toast.makeText(context, R.string.contact_not_added, Toast.LENGTH_LONG).show()
          currentlyAdding -= remote
        }
      case _ =>
    }
  }

  /**
   * Adds the given address as a new contact, if local and remote device sent a [[ResultAddContact]]
   * message with accepted = true.
   */
  private def addContactIfBothConfirmed(address: Address): Unit = {
    val info = currentlyAdding(address)
    val user = getUser(address)
    if (info.localConfirmed && info.remoteConfirmed) {
      Log.i(Tag, "Adding new contact " + user.toString)
      database.addContact(user)
      Toast
        .makeText(context, context.getString(R.string.contact_added, user.name), Toast.LENGTH_SHORT)
        .show()
      currentlyAdding -= address
    }
  }

}
