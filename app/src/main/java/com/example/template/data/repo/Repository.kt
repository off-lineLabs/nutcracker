package com.example.template.data.repo

import com.example.template.data.model.CheckIn
import com.example.template.data.model.Piece

interface Repository {
	fun getGoal(): Int
	fun setGoal(value: Int)

	fun getPieces(): List<Piece>
	fun addPiece(piece: Piece)

	fun getCheckIns(): List<CheckIn>
	fun addCheckIn(checkIn: CheckIn)
}

class InMemoryRepository(initialGoal: Int = 2000) : Repository {
	private var goal: Int = initialGoal
	private val pieces = mutableListOf<Piece>()
	private val checkIns = mutableListOf<CheckIn>()

	override fun getGoal(): Int = goal

	override fun setGoal(value: Int) {
		goal = value
	}

	override fun getPieces(): List<Piece> = pieces.toList()

	override fun addPiece(piece: Piece) {
		pieces.add(piece)
	}

	override fun getCheckIns(): List<CheckIn> = checkIns.toList()

	override fun addCheckIn(checkIn: CheckIn) {
		checkIns.add(0, checkIn)
	}
}


