package org.frice.platform.owners

import org.frice.obj.Collidable
import org.frice.platform.FriceImage
import org.frice.utils.shape.FShapeQuad

interface ImageOwner : Collidable, FShapeQuad {
	val image: FriceImage

	override val width: Double get() = image.width.toDouble()
	override val height: Double get() = image.height.toDouble()

	var collisionBox: FShapeQuad?
	override val box: FShapeQuad get() = collisionBox ?: this
}