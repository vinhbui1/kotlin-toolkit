/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.pdf2epub

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import org.readium.r2.pdf2epub.R
import org.readium.r2.pdf2epub.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var topProgressBar: ProgressBar

    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()

    fun showProgressBar() {
        topProgressBar.visibility = View.VISIBLE
    }

    fun hideProgressBar() {
        topProgressBar.visibility = View.GONE
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        topProgressBar = findViewById(R.id.topProgressBar)


      //  val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_bookshelf,
                R.id.navigation_catalog_list,
//                R.id.navigation_about
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
     //   navView.setupWithNavController(navController)

        viewModel.channel.receive(this) { handleEvent(it) }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun handleEvent(event: MainViewModel.Event) {
        when (event) {
            is MainViewModel.Event.ImportPublicationSuccess -> {
                hideProgressBar()
                Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.import_publication_success),
                    Snackbar.LENGTH_LONG
                ).show()
            }
            is MainViewModel.Event.ImportPublicationError -> {
                event.error.toUserError().show(this)
            }

            MainViewModel.Event.ImportPublicationInit ->{

                showProgressBar()
                Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.import_publication_init),
                    Snackbar.LENGTH_LONG
                ).show()
            }


            MainViewModel.Event.CreateEpubFromText ->  Snackbar.make(
                findViewById(android.R.id.content),
                "Creating epub from text",
                Snackbar.LENGTH_LONG
            ).show()
            MainViewModel.Event.ExtractTextFromPdf ->  Snackbar.make(
                findViewById(android.R.id.content),
                "Extracting Text from PDF",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
}
