package com.banxian.bannerwidget;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.view.ViewPager.PageTransformer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.banxian.bannerwidget.transformer.DepthPageTransformer;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class BannerView extends FrameLayout {
	private ViewPager mPager;
	// 因这个不支持无限循环,所以弃用
	// private CirclePageIndicator mPageIndicator;
	private ImageLoader imageLoader = ImageLoader.getInstance();
	private DisplayImageOptions options; // DisplayImageOptions是用于设置图片显示的类

	private String[] imgUris;
	private ImagePagerAdapter pagerAdapter;
	// 点点图片
	private ImageView[] tips;
	// 放点点的容器
	private ViewGroup tipsContainer;
	private BannerView.OnItemClickListener l;

	// 间隔时间
	// private static final long SPANTIME = 3000;
	// 定时滚动timer
	private Timer slideTimer;
	/** 是否允许自动滚动 */
	private boolean isCanAutoSlide = true;
	/** 判断是不是点击行为 */
	private float firstDownX;
	private float firstDownY;
	private float lastDownX;
	private float lastDownY;
	/******************/
	/** 没有读取出来的时候显示的图片 */
	private static int loadImgRes = 0;

	public BannerView(Context context) {
		super(context);
		initUILConfig();
		init(context);
	}

	public BannerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initUILConfig();
		init(context);
	}

	private void init(Context context) {
		LayoutInflater.from(context).inflate(R.layout.view_banner, this);
		mPager = (ViewPager) findViewById(R.id.pager);
		// mPageIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
		tipsContainer = (ViewGroup) findViewById(R.id.viewGroup);
		pagerAdapter = new ImagePagerAdapter();
		mPager.setAdapter(pagerAdapter);
		setPagerSpeed();
		// 使用DisplayImageOptions.Builder()创建DisplayImageOptions
		options = new DisplayImageOptions.Builder().cacheInMemory(true)
		// 设置下载的图片是否缓存在内存中
				.cacheOnDisc(true) // 设置下载的图片是否缓存在SD卡中
				.resetViewBeforeLoading(true).build(); // 创建配置过得DisplayImageOption对象
		// mPager.setAdapter(mPagerAdapter);
		// mPageIndicator.setViewPager(mPager);
		// mPager.setPageTransformer(true, new ZoomOutPageTransformer());
		// the other transformer
		mPager.setPageTransformer(true, new DepthPageTransformer());
		mPager.setCurrentItem(300);
		mPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				int selectItem = position % imgUris.length;
				for (int i = 0; i < tips.length; i++) {
					if (i == selectItem) {
						tips[i].setBackgroundResource(R.drawable.page_indicator_focused);
					} else {
						tips[i].setBackgroundResource(R.drawable.page_indicator_unfocused);
					}
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {

			}
		});

		mPager.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					firstDownX = event.getX();
					firstDownY = event.getY();
					isCanAutoSlide = false;
					break;
				case MotionEvent.ACTION_MOVE:
					lastDownX = event.getX();
					lastDownY = event.getY();
					isCanAutoSlide = false;
					break;
				case MotionEvent.ACTION_UP:
					isCanAutoSlide = true;
					float deltaX = Math.abs(lastDownX - firstDownX);
					float deltaY = Math.abs(lastDownY - firstDownY);
					Log.v("ts", "变化的xy:" + deltaX + "," + deltaY);
					if (deltaX < 10 && deltaY < 10 && null != l) {
						l.OnItemClick(mPager.getCurrentItem() % imgUris.length);
					}
					break;
				}
				// Log.v("ts", "action:" + event.getAction() + ",canSlide?:"
				// + isCanAutoSlide);
				return false;
			}
		});
		// new Timer().schedule(bannerTimer, SPANTIME, SPANTIME);
	}

	private void initUILConfig() {
		File cacheDir = StorageUtils.getOwnCacheDirectory(getContext(),
				"muil/cache");

		// 1.new Md5FileNameGenerator() //使用MD5对UIL进行加密命名
		// 2.new HashCodeFileNameGenerator()//使用HASHCODE对UIL进行加密命名

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getContext()).threadPriority(Thread.NORM_PRIORITY - 2)
				.denyCacheImageMultipleSizesInMemory()
				.discCacheFileNameGenerator(new Md5FileNameGenerator())
				.tasksProcessingOrder(QueueProcessingType.LIFO)
				// 先进先出FIFO,后进先出LIFO,因为这个是图片浏览用的,所以要后进先出
				.discCache(new UnlimitedDiscCache(cacheDir))
				// 自定义的缓存路径
				.imageDownloader(
						new BaseImageDownloader(getContext(), 5 * 1000,
								30 * 1000)) // connectTimeout (5 s), readTimeout
											// (30 s)超时时间
				.writeDebugLogs() // Remove for release app
				.build();
		// .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
		// .memoryCacheSize(2 * 1024 * 1024) //内存限制2m
		// .discCacheSize(50 * 1024 * 1024)
		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);
	}

	/**
	 * 设置图片URI
	 * 
	 * @param uris
	 *            URI 格式如下: 网络图片地址 或 assets://+图片全名 或 drawable://+图片ID
	 */
	public void setImgUris(String[] uris) {
		this.imgUris = uris;
		// 将位置点加入到banner
		tips = new ImageView[imgUris.length];
		for (int i = 0; i < tips.length; i++) {
			ImageView imageView = new ImageView(getContext());
			imageView
					.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
							10, 10));
			tips[i] = imageView;
			if (i == 0)
				tips[i].setBackgroundResource(R.drawable.page_indicator_focused);
			else
				tips[i].setBackgroundResource(R.drawable.page_indicator_unfocused);

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					new ViewGroup.LayoutParams(
							android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
							android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
			params.leftMargin = 5;
			params.rightMargin = 5;
			tipsContainer.addView(imageView, params);
		}
		pagerAdapter.notifyDataSetChanged();
	}

	/**
	 * 设置滚动特效
	 * 
	 * @param transformer
	 */
	public void setPageTransformer(PageTransformer transformer) {
		if (null != mPager)
			mPager.setPageTransformer(true, transformer);
	}

	/**
	 * 开始滚动
	 * 
	 * @param spanTime
	 *            滚动间隔时间(ms,毫秒)
	 */
	public void startRoll(long spanTime) {
		if (null == slideTimer) {
			slideTimer = new Timer();
			slideTimer.schedule(new RollTimerTask(), spanTime, spanTime);
		}
	}

	/**
	 * 停止滚动
	 */
	public void stopRoll() {
		if (null != slideTimer) {
			// bannerTimer.cancel();
			// slideTimer.purge();
			slideTimer.cancel();
			slideTimer = null;
		}
	}

	public boolean isRolling() {
		return null != slideTimer;
	}

	/**
	 * 清理内存缓存
	 */
	public void clearMemoryCache() {
		imageLoader.clearMemoryCache();
	}

	/**
	 * 清理sd卡缓存
	 */
	public void clearDiscCache() {
		imageLoader.clearDiscCache();
	}

	public void setOnItemClickListener(BannerView.OnItemClickListener l) {
		this.l = l;
	}

	public void setDisplayImageOptions(DisplayImageOptions options) {
		this.options = options;
	}

	/**
	 * 设置未读取完成状态下显示的默认图片ID
	 * 
	 * @param imgRes
	 */
	@SuppressWarnings("static-access")
	public void setDefaultImgRes(int imgRes) {
		this.loadImgRes = imgRes;
	}

	/**
	 * 通过反射设置viewpager.setcurrentItem的滚动速度
	 */
	private void setPagerSpeed() {
		try {
			Field mScroller;
			mScroller = ViewPager.class.getDeclaredField("mScroller");
			mScroller.setAccessible(true);
			FixedSpeedScroller scroller = new FixedSpeedScroller(
					mPager.getContext(), new LinearInterpolator());
			// scroller.setFixedDuration(5000);
			mScroller.set(mPager, scroller);
		} catch (NoSuchFieldException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
	}

	private class ImagePagerAdapter extends PagerAdapter {

		// private String[] images;
		private LayoutInflater inflater;

		ImagePagerAdapter() {
			// this.images = images;
			inflater = LayoutInflater.from(getContext());
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager) container).removeView((View) object);
		}

		@Override
		public void finishUpdate(View container) {
		}

		@Override
		public int getCount() {
			// if (null == imgUris)
			// return 0;
			// return imgUris.length;
			return Integer.MAX_VALUE;
		}

		@Override
		public Object instantiateItem(ViewGroup view, int position) {
			View imageLayout = inflater.inflate(R.layout.fragment_image, view,
					false);
			final ImageView imageView = (ImageView) imageLayout
					.findViewById(R.id.fragment_imageview);
			imageView.setScaleType(ScaleType.FIT_XY);
			final int p = position % imgUris.length;
			imageLoader.displayImage(imgUris[p], imageView, options,
					new AnimateFirstDisplayListener());

			// imageLayout.setOnClickListener(new OnClickListener() {
			//
			// @Override
			// public void onClick(View v) {
			// if (null == l)
			// return;
			// l.OnItemClick(imageView, mPager.getCurrentItem()
			// % imgUris.length);
			// }
			// });

			((ViewPager) view).addView(imageLayout, 0); // 将图片增加到ViewPager
			return imageLayout;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view.equals(object);
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View container) {
		}
	}

	private class RollTimerTask extends TimerTask {
		@Override
		public void run() {
			if (!isCanAutoSlide)
				return;
			if (null == imgUris)
				return;
			int count = imgUris.length;
			if (count > 1) { // 多于1个，才循环
				Message message = new Message();
				int index = mPager.getCurrentItem();
				message.what = (index + 1) % Integer.MAX_VALUE;
				// mPager.setCurrentItem(index, true);
				handler.sendMessage(message);
			}
		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			mPager.setCurrentItem(msg.what, true);
		};
	};

	/**
	 * 图片加载第一次显示监听器
	 * 
	 * @author Administrator
	 * 
	 */
	private static class AnimateFirstDisplayListener implements
			ImageLoadingListener {

		static final List<String> displayedImages = Collections
				.synchronizedList(new LinkedList<String>());

		@Override
		public void onLoadingStarted(String imageUri, View view) {
			if (loadImgRes != 0) {
				ImageView imageView = (ImageView) view;
				if (null != imageView){
					imageView.setBackgroundResource(loadImgRes);  //不能用setImageResource
				}
			}
		}

		@Override
		public void onLoadingFailed(String imageUri, View view,
				FailReason failReason) {

		}

		@Override
		public void onLoadingCancelled(String imageUri, View view) {

		}

		@Override
		public void onLoadingComplete(String imageUri, View view,
				Bitmap loadedImage) {
			if (loadedImage != null) {
				ImageView imageView = (ImageView) view;
				// 是否第一次显示
				boolean firstDisplay = !displayedImages.contains(imageUri);
				if (firstDisplay) {
					// 图片淡入效果
					FadeInBitmapDisplayer.animate(imageView, 500);
					displayedImages.add(imageUri);
				}
			}
		}
	}

	public interface OnItemClickListener {
		public void OnItemClick(int position);
	}
}
