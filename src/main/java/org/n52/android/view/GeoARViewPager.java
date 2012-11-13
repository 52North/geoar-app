package org.n52.android.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class GeoARViewPager extends ViewPager {

	private List<Fragment> fragments = new ArrayList<Fragment>();
	private GeoARPagerAdpter mPagerAdapter;

	public GeoARViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GeoARViewPager(Context context) {
		super(context);
	}

	public void setFragmentManager(FragmentManager fm) {
		mPagerAdapter = new GeoARPagerAdpter(fm);
		setAdapter(mPagerAdapter);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return false;
	}

	public void showFragment(Fragment fragment) {
		setCurrentItem(fragments.indexOf(fragment));
	}

	public Fragment getCurrentFragment() {
		return fragments.get(getCurrentItem());
	}

	public void addFragment(Fragment fragment) {
		fragments.add(fragment);
	}

	private class GeoARPagerAdpter extends FragmentStatePagerAdapter {

		public GeoARPagerAdpter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getCount() {
			return fragments.size();
		}

	}
}
