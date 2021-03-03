package io.haringsrob.hidetitlebar

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import io.haringsrob.hidetitlebar.laf.LookAndFeelInstaller.installAllUIComponents

class HideTitleBar : Disposable {
  private val connection = ApplicationManager.getApplication().messageBus.connect()

  init {
    installAllUIComponents()

    connection.subscribe(
      LafManagerListener.TOPIC,
      LafManagerListener {
        installAllUIComponents()
      }
    )
  }

  override fun dispose() {
    connection.dispose()
  }
}
