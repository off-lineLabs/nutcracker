package com.example.template.data.model

import java.util.UUID

data class Piece(
	val name: String,
	val value: Int,
	val id: String = UUID.randomUUID().toString()
)

data class CheckIn(
	val pieceName: String,
	val pieceId: String,
	val percentage: Double,
	val timestamp: String,
	val id: String = UUID.randomUUID().toString()
)

data class Macro(
	val carbs: Int,
	val protein: Int,
	val fat: Int,
	val fiber: Int
)


