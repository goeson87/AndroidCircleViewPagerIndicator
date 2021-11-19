package goeson.circleviewpagerindicator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import goeson.circleviewpagerindicator.view.CircleViewPagerIndicator

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pager = findViewById<ViewPager>(R.id.viewPager)
            .apply {
                adapter = ViewPagerAdapter()
            }

        findViewById<CircleViewPagerIndicator>(R.id.viewPagerIndicator).setViewPager(pager)
    }

    private class ViewPagerAdapter: PagerAdapter() {

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            return LayoutInflater.from(container.context).inflate(R.layout.layout_page, container, false)
                .apply {
                    findViewById<TextView>(R.id.text).text = (position + 1).toString()
                }.also {
                    (container as ViewPager).addView(it)
                }
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getCount(): Int = 5

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return (view == `object`)
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            (container as ViewPager).removeView(`object` as View)
        }

    }
}