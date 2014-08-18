package com.banxian.bannerwidget;

import java.io.File;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class MainActivity extends FragmentActivity {
	private BannerView bannerView;
	private final int[] images = new int[] { R.drawable.test1,
			R.drawable.test2, R.drawable.test3, R.drawable.test4,
			R.drawable.test5, R.drawable.test6, R.drawable.test7,
			R.drawable.test8, R.drawable.test9, R.drawable.test10,
			R.drawable.test11, R.drawable.test12 };

	private final int[] images_small = new int[] { R.drawable.test14,
			R.drawable.test15, R.drawable.test16, R.drawable.test17,
			R.drawable.test18, R.drawable.test19, R.drawable.test20,
			R.drawable.test21, R.drawable.test22, R.drawable.test23,
			R.drawable.test24, R.drawable.test25 };

	public static final String[] images_net = new String[] {
			// Heavy images

			"http://img1.gamedog.cn/2013/07/11/43-130G1150H00.jpg",
			"http://img1.gamedog.cn/2013/07/11/43-130G1150F80.jpg",
			"http://h.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=4fd68ed9013b5bb5bed724fd00e5ee5a/1c950a7b02087bf46d33d89bf0d3572c10dfcfe1.jpg",
			"http://img6.3lian.com/c23/desk3/25/04/30.jpg",
			"http://f.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=63de1291ff1f4134e037017d1329aea9/b90e7bec54e736d1eaee037a99504fc2d46269c7.jpg",
			"http://img4.duitang.com/uploads/item/201205/14/20120514124625_yEZfh.thumb.600_0.jpeg",
			"http://a.hiphotos.baidu.com/zhidao/pic/item/e850352ac65c1038bb9e8cd4b0119313b17e89c0.jpg",
			"http://img.159.com/theme/pic/2011/3/23/20113231519330.gif",
			"http://img.159.com/desk/user/2012/2/16/Jiker201214232116125.jpg",
			"http://img1.gamedog.cn/2013/07/13/43-130G31514190-50.jpg",
			"http://img1.gamedog.cn/2013/07/12/43-130G2144U80.jpg",
			"http://img1.gamedog.cn/2013/07/11/44-130G10U6130.jpg",
			"http://img1.gamedog.cn/2013/07/11/44-130G10U6120-50.jpg",
			"http://img1.gamedog.cn/2013/07/18/43-130GQ639490.jpg",
			"http://f2.dn.anqu.com/down/OGYzOQ==/allimg/1306/16-130603102352.jpg",
			"http://f.hiphotos.baidu.com/zhidao/pic/item/94cad1c8a786c91722363af3c83d70cf3ac757c6.jpg",
			"http://attach.bbs.miui.com/forum/201306/13/112333epx9un42z8ed9pwp.jpg" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bannerView = (BannerView) findViewById(R.id.bannerView1);
		bannerView.setImgUris(drawableToUri(images_small));
		// bannerView.setPageTransformer(new ZoomOutPageTransformer());
		findViewById(R.id.btn).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (bannerView.isRolling())
					bannerView.stopRoll();
				else
					bannerView.startRoll(3000);
			}
		});

		findViewById(R.id.btn_clear1).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				bannerView.clearMemoryCache();
			}
		});

		findViewById(R.id.btn_clear2).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				bannerView.clearDiscCache();
			}
		});

		bannerView.setOnItemClickListener(new BannerView.OnItemClickListener() {

			@Override
			public void OnItemClick(int position) {
				Toast.makeText(MainActivity.this, "点击" + position, 0).show();
			}
		});

//		DisplayImageOptions options = new DisplayImageOptions.Builder()
//				.showImageForEmptyUri(R.drawable.test).build();
//		bannerView.setDisplayImageOptions(options);
		bannerView.setDefaultImgRes(R.drawable.test);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		bannerView.clearMemoryCache(); // 清除内存缓存
		bannerView.clearDiscCache(); // 清除SD卡中的缓存
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String[] drawableToUri(int[] imgIds) {
		String[] res = new String[imgIds.length];
		for (int i = 0; i < imgIds.length; i++) {
			StringBuilder sb = new StringBuilder();
			sb.append("drawable://");
			sb.append(imgIds[i]);
			res[i] = sb.toString();
		}
		return res;
	}

}
