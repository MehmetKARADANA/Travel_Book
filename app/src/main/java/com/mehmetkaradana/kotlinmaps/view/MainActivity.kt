package com.mehmetkaradana.kotlinmaps.view

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.mehmetkaradana.kotlinmaps.R
import com.mehmetkaradana.kotlinmaps.adapter.PlaceAdapter
import com.mehmetkaradana.kotlinmaps.databinding.ActivityMainBinding
import com.mehmetkaradana.kotlinmaps.model.Place
import com.mehmetkaradana.kotlinmaps.roomdb.PlaceDao
import com.mehmetkaradana.kotlinmaps.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
      //  enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        setContentView(view)

        val db = Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places")
            //  .allowMainThreadQueries()
            .build()
       val placeDao = db.PlaceDao()

        compositeDisposable.add(
            placeDao.getAll()
                .subscribeOn(Schedulers.io())//scheduler.io da işlemi yap
                .observeOn(AndroidSchedulers.mainThread())//main threadde izle
                .subscribe(this::handleRequest)//responsetan sonra bunu yap
        )
    }

   private fun handleRequest(list: List<Place>){
       //to do: recyclerview için adapter yaz
       binding.recyclerView.layoutManager = LinearLayoutManager(this)
       val adapter = PlaceAdapter(list)
       //adapterlar ne yapar ?
       binding.recyclerView.adapter=adapter
   }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //menu xmli ile kodu bağlamak için
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.place_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //item seçildiğinde ne olucak
        if(item.itemId== R.id.add_place){
            val intent=Intent(this, MapsActivity::class.java)
            intent.putExtra("info", "new")
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}