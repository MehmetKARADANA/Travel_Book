package com.mehmetkaradana.kotlinmaps.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mehmetkaradana.kotlinmaps.model.Place

@Database(entities = [Place::class], version = 1)
abstract class PlaceDatabase : RoomDatabase() {
    abstract fun PlaceDao(): PlaceDao
}