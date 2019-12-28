package org.frice

import com.sun.javafx.tk.Toolkit
import javafx.application.Application
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.scene.*
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.scene.text.Font
import javafx.stage.Stage
import org.frice.event.*
import org.frice.obj.button.FText
import org.frice.platform.*
import org.frice.platform.adapter.JfxDrawer
import org.frice.platform.adapter.JvmImage
import org.frice.resource.graphics.ColorResource
import org.frice.util.EventManager
import org.frice.util.shape.FRectangle
import org.frice.util.shape.FShapeQuad
import org.frice.util.time.*
import java.util.*
import kotlin.concurrent.thread

/**
 * The base game class using JavaFX as renderer (recommended).
 * This class does rendering jobs, and something which are
 * invisible to game developers.
 *
 * Feel free to override the constructor.
 *
 * @author ice1000
 * @since v1.5.0
 */
open class GameFX @JvmOverloads constructor(
	layerCount: Int = 1,
	private val width: Int = 800,
	private val height: Int = 800) : Application(), FriceGame {

	override val eventManager = EventManager()
	override var activeArea: FShapeQuad? = null

	override fun isResizable() = stage.isResizable
	override fun setResizable(resizable: Boolean) {
		stage.isResizable = resizable
	}

	override fun getWidth() = width
	override fun getHeight() = height

	override var isFullScreen: Boolean
		get() = stage.isFullScreen
		set(value) {
			stage.isFullScreen = value
		}

	override var isAlwaysTop: Boolean
		get() = stage.isAlwaysOnTop
		set(value) {
			stage.isAlwaysOnTop = value
		}

	override var paused = false
		set(value) {
			if (value) FClock.pause() else FClock.resume()
			field = value
		}

	override var stopped = false
		set(value) {
			if (value) FClock.pause() else FClock.resume()
			field = value
		}

	override val layers = Array(layerCount) { Layer() }
	override var debug = true
	override var showFPS = true
	final override var loseFocus = false

	private lateinit var stage: Stage
	private lateinit var scene: Scene

	private val fpsCounter = FpsCounter()

	override var loseFocusChangeColor = true

	private val refresh = FTimer(4)
	override var millisToRefresh: Int
		get () = refresh.time
		set (value) {
			refresh.time = value
		}

	private fun <T> initAlert(it: Dialog<T>, title: String) {
		it.isResizable = false
		it.title = title
		it.showAndWait()
	}

	override fun dialogConfirmYesNo(msg: String, title: String): Boolean =
		Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO).let {
			initAlert(it, title)
			return@let it.result == ButtonType.YES
		}

	override fun dialogShow(msg: String, title: String) {
		Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).let { initAlert(it, title) }
	}

	override fun dialogInput(msg: String, title: String): String = TextInputDialog().let {
		initAlert(it, title)
		return@let it.result
	}

	override val screenCut
		get() = JvmImage(SwingFXUtils.fromFXImage(
			canvas.snapshot(SnapshotParameters(), null), null))

	override fun getTitle(): String = stage.title
	override fun setTitle(title: String) {
		stage.title = title
	}

	override fun measureText(text: FText): FRectangle {
		drawer.useFont(text)
		val font: Font = text.`font tmp obj` as? Font ?: drawer.g.font
		return FRectangle(Toolkit.getToolkit().fontLoader.computeStringWidth(text.text, font),
			Toolkit.getToolkit().fontLoader.getFontMetrics(font).lineHeight)
	}

	override fun measureTextHeight(text: FText): Int {
		drawer.useFont(text)
		val font: Font = text.`font tmp obj` as? Font ?: drawer.g.font
		return Toolkit.getToolkit().fontLoader.getFontMetrics(font).lineHeight.toInt()
	}

	override fun measureTextWidth(text: FText): Int {
		val font: Font = text.`font tmp obj` as? Font ?: drawer.g.font
		return Toolkit.getToolkit().fontLoader.computeStringWidth(text.text, font).toInt()
	}

	var onKeyTyped: EventHandler<in KeyEvent>
		get() = scene.onKeyTyped
		set(value) {
			scene.onKeyTyped = value
		}

	var onKeyPressed: EventHandler<in KeyEvent>
		get() = scene.onKeyPressed
		set(value) {
			scene.onKeyPressed = value
		}

	var onKeyReleased: EventHandler<in KeyEvent>
		get() = scene.onKeyReleased
		set(value) {
			scene.onKeyReleased = value
		}

	val canvas = Canvas(width.toDouble(), height.toDouble())
	val root = StackPane(canvas)
	private val drawer = JfxDrawer(canvas.graphicsContext2D)

	override fun setCursor(o: FriceImage) {
		scene.cursor = ImageCursor(o.fx().jfxImage)
	}

	open fun onExit() = !dialogConfirmYesNo("Are you sure to exit?")

	final override fun start(stage: Stage) {
		this.stage = stage
		scene = Scene(root, width.toDouble(), height.toDouble())
		isResizable = false
		scene.setOnMouseClicked { mouse(fxMouse(it, MOUSE_CLICKED)) }
		scene.setOnMousePressed { mouse(fxMouse(it, MOUSE_PRESSED)) }
		scene.setOnMouseMoved { mouse(fxMouse(it, MOUSE_MOVED)) }
		scene.setOnMouseReleased { mouse(fxMouse(it, MOUSE_RELEASED)) }
		stage.setOnCloseRequest { if (onExit()) it.consume() else stopped = true }
		stage.scene = scene
		onInit()
		stage.icons += Image(javaClass.getResourceAsStream("/icon.png"))
		stage.show()
		thread {
			onLastInit()
			while (true) {
				try {
					adjust()
					if (stopped) break
					if (!paused and refresh.ended()) {
						clearScreen(drawer)
						dealWithObjects(drawer)
						drawer.init()
						drawer.color = ColorResource.DARK_GRAY
						fpsCounter.refresh()
						if (showFPS) drawer.drawString("fps: ${fpsCounter.display}", 30.0, height - 30.0)
					}
				} catch (ignored: ConcurrentModificationException) {
				}
			}
		}
	}
}
