package io.haringsrob.hidetitlebar.ui

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeBasedLookAndFeelInfo
import com.intellij.ide.ui.laf.darcula.ui.DarculaRootPaneUI
import com.intellij.openapi.util.SystemInfo.*
import com.intellij.util.ui.JBUI.insets
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.UIUtil.getWindow
import io.haringsrob.hidetitlebar.util.toOptional
import java.awt.Component
import java.awt.Insets
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.beans.PropertyChangeListener
import java.util.*
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JRootPane
import javax.swing.border.AbstractBorder
import javax.swing.plaf.ComponentUI
import javax.swing.plaf.basic.BasicRootPaneUI

typealias Disposer = () -> Unit

class TitlePaneUI : DarculaRootPaneUI() {

  companion object {
    const val LOL_NOPE = "This should not be shown"
    const val BLANK = " "
    private const val defaultPane = "com.sun.java.swing.plaf.windows.WindowsRootPaneUI"
    const val WINDOW_DARK_APPEARANCE = "jetbrains.awt.windowDarkAppearance"
    const val TRANSPARENT_TITLE_BAR_APPEARANCE = "jetbrains.awt.transparentTitleBarAppearance"

    @JvmStatic
    @Suppress("ACCIDENTAL_OVERRIDE", "UNUSED", "UNUSED_PARAMETER")
    fun createUI(component: JComponent): ComponentUI =
      if (hasTransparentTitleBar()) {
        TitlePaneUI()
      } else {
        createDefaultRootPanUI()
      }

    private fun createDefaultRootPanUI(): ComponentUI = try {
      Class.forName(defaultPane).getConstructor().newInstance() as ComponentUI
    } catch (e: Throwable) {
      BasicRootPaneUI()
    }

    private fun hasTransparentTitleBar(): Boolean = isMac
  }

  private var possibleDisposable: Optional<Disposer> = Optional.empty()

  override fun uninstallUI(c: JComponent?) {
    super.uninstallUI(c)
    possibleDisposable.ifPresent { it() }
  }

  override fun installUI(c: JComponent?) {
    super.installUI(c)

    LafManager.getInstance().currentLookAndFeel
      .toOptional()
      .filter { isMac || isLinux }
      .ifPresent {
        val isDark =
          if (it is UIThemeBasedLookAndFeelInfo) it.theme.isDark
          else StartupUiUtil.isUnderDarcula()
        c?.putClientProperty(WINDOW_DARK_APPEARANCE, isDark)
        val rootPane = c as? JRootPane
        attemptTransparentTitle(c) { shouldBeTransparent ->
          c?.putClientProperty(TRANSPARENT_TITLE_BAR_APPEARANCE, shouldBeTransparent)
          if (shouldBeTransparent) {
            setThemedTitleBar(
              getWindow(c),
              rootPane
            )
          } else {
            {}
          }
        }() { disposer ->
          possibleDisposable = disposer.toOptional()
        }
      }
  }

  private fun setThemedTitleBar(
    window: Window?,
    rootPane: JRootPane?
  ): () -> Unit {
    val customDecorationBorder = object : AbstractBorder() {
      override fun getBorderInsets(c: Component?): Insets {
        return insets(0)
      }
    }
    rootPane?.border = customDecorationBorder

    val windowAdapter: WindowAdapter = object : WindowAdapter() {
      override fun windowActivated(e: WindowEvent) {
        rootPane?.repaint()
      }

      override fun windowDeactivated(e: WindowEvent) {
        rootPane?.repaint()
      }
    }

    val changeListener = PropertyChangeListener { rootPane?.repaint() }
    window?.addPropertyChangeListener("title", changeListener)
    return {
      window?.removeWindowListener(windowAdapter)
      window?.removePropertyChangeListener(changeListener)
    }
  }

  private fun attemptTransparentTitle(
    component: JComponent?,
    handleIsTransparent: (Boolean) -> () -> Unit
  ): ((Disposer) -> Unit) -> Unit {
    return if (Runtime.version().feature() < 11) {
      { resolve ->
        resolve(handleIsTransparent(true))
      }
    } else {
      return { resolve ->
        component?.addHierarchyListener {
          val window = getWindow(component)
          val title = getTitle(window)
          resolve(handleIsTransparent(title != LOL_NOPE))
        }
      }
    }
  }

  private fun getTitle(window: Window?): String =
    when (window) {
      is JDialog -> window.title
      is JFrame -> window.title
      else -> LOL_NOPE
    } ?: BLANK .ifEmpty {
      BLANK
    }
}
