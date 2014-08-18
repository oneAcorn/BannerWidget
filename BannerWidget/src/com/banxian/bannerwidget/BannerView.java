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
	// �������֧������ѭ��,��������
	// private CirclePageIndicator mPageIndicator;
	private ImageLoader imageLoader = ImageLoader.getInstance();
	private DisplayImageOptions options; // DisplayImageOptions����������ͼƬ��ʾ����

	private String[] imgUris;
	private ImagePagerAdapter pagerAdapter;
	// ���ͼƬ
	private ImageView[] tips;
	// �ŵ�������
	private ViewGroup tipsContainer;
	private BannerView.OnItemClickListener l;

	// ���ʱ��
	// private static final long SPANTIME = 3000;
	// ��ʱ����timer
	private Timer slideTimer;
	/** �Ƿ������Զ����� */
	private boolean isCanAutoSlide = true;
	/** �ж��ǲ��ǵ����Ϊ */
	private float firstDownX;
	private float firstDownY;
	private float lastDownX;
	private float lastDownY;
	/******************/
	/** û�ж�ȡ������ʱ����ʾ��ͼƬ */
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
		// ʹ��DisplayImageOptions.Builder()����DisplayImageOptions
		options = new DisplayImageOptions.Builder().cacheInMemory(true)
		// �������ص�ͼƬ�Ƿ񻺴����ڴ���
				.cacheOnDisc(true) // �������ص�ͼƬ�Ƿ񻺴���SD����
				.resetViewBeforeLoading(true).build(); // �������ù���DisplayImageOption����
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
					Log.v("ts", "�仯��xy:" + deltaX + "," + deltaY);
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

		// 1.new Md5FileNameGenerator() //ʹ��MD5��UIL���м�������
		// 2.new HashCodeFileNameGenerator()//ʹ��HASHCODE��UIL���м�������

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getContext()).threadPriority(Thread.NORM_PRIORITY - 2)
				.denyCacheImageMultipleSizesInMemory()
				.discCacheFileNameGenerator(new Md5FileNameGenerator())
				.tasksProcessingOrder(QueueProcessingType.LIFO)
				// �Ƚ��ȳ�FIFO,����ȳ�LIFO,��Ϊ�����ͼƬ����õ�,����Ҫ����ȳ�
				.discCache(new UnlimitedDiscCache(cacheDir))
				// �Զ���Ļ���·��
				.imageDownloader(
						new BaseImageDownloader(getContext(), 5 * 1000,
								30 * 1000)) // connectTimeout (5 s), readTimeout
											// (30 s)��ʱʱ��
				.writeDebugLogs() // Remove for release app
				.build();
		// .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
		// .memoryCacheSize(2 * 1024 * 1024) //�ڴ�����2m
		// .discCacheSize(50 * 1024 * 1024)
		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);
	}

	/**
	 * ����ͼƬURI
	 * 
	 * @param uris
	 *            URI ��ʽ����: ����ͼƬ��ַ �� assets://+ͼƬȫ�� �� drawable://+ͼƬID
	 */
	public void setImgUris(String[] uris) {
		this.imgUris = uris;
		// ��λ�õ���뵽banner
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
	 * ���ù�����Ч
	 * 
	 * @param transformer
	 */
	public void setPageTransformer(PageTransformer transformer) {
		if (null != mPager)
			mPager.setPageTransformer(true, transformer);
	}

	/**
	 * ��ʼ����
	 * 
	 * @param spanTime
	 *            �������ʱ��(ms,����)
	 */
	public void startRoll(long spanTime) {
		if (null == slideTimer) {
			slideTimer = new Timer();
			slideTimer.schedule(new RollTimerTask(), spanTime, spanTime);
		}
	}

	/**
	 * ֹͣ����
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
	 * �����ڴ滺��
	 */
	public void clearMemoryCache() {
		imageLoader.clearMemoryCache();
	}

	/**
	 * ����sd������
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
	 * ����δ��ȡ���״̬����ʾ��Ĭ��ͼƬID
	 * 
	 * @param imgRes
	 */
	@SuppressWarnings("static-access")
	public void setDefaultImgRes(int imgRes) {
		this.loadImgRes = imgRes;
	}

	/**
	 * ͨ����������viewpager.setcurrentItem�Ĺ����ٶ�
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

			((ViewPager) view).addView(imageLayout, 0); // ��ͼƬ���ӵ�ViewPager
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
			if (count > 1) { // ����1������ѭ��
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
	 * ͼƬ���ص�һ����ʾ������
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
					imageView.setBackgroundResource(loadImgRes);  //������setImageResource
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
				// �Ƿ��һ����ʾ
				boolean firstDisplay = !displayedImages.contains(imageUri);
				if (firstDisplay) {
					// ͼƬ����Ч��
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
