package baxermux.server.http;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ActivityManager.RunningServiceInfo;
import android.widget.CompoundButton.OnCheckedChangeListener;


public class MainActivity extends Activity
{

	// �s��@�Ϊ���
	private HttpService mBoundService;
	boolean mIsBound;

	// ui����
	TextView server_status_text, ip_status_text;
	Switch server_turn;
	Button debug_button;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Log.i("my", "app start");
		
		// UI�����l�ƫ���
		server_status_text = (TextView) this.findViewById(R.id.server_status_textview);
		ip_status_text = (TextView) this.findViewById(R.id.ip_status_textview);
		server_turn = (Switch) this.findViewById(R.id.server_switch);
		debug_button = (Button) this.findViewById(R.id.debug);

		if (isMyServiceRunning() == true)
		{
			server_status_text.setText("http service �Ұʤ�..");
			doBindService();
			server_turn.setChecked(true);
		}
		else
			server_status_text.setText("http service �|���Ұ�");

		ip_status_text.setText("�ثe IP : " + Utils.getIPAddress(true));

		server_turn.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					doBindService();

					// ref http://stackoverflow.com/questions/8897535/android-socket-gets-killed-imidiatelly-after-screen-goes-blank/18916511#18916511
					WifiManager wMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
					WifiManager.WifiLock wifiLock = wMgr.createWifiLock(WifiManager.WIFI_MODE_FULL, "MyWifiLock");
					PowerManager pMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
					PowerManager.WakeLock wakeLock = pMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");

					try
					{
						wifiLock.acquire();
					}
					catch (Exception e)
					{
					}
					try
					{
						wakeLock.acquire();
					}
					catch (Exception e)
					{
					}
				}
				else
				{
					doUnbindService();
					stopService(new Intent(MainActivity.this, HttpService.class));

					WifiManager wMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
					WifiManager.WifiLock wifiLock = wMgr.createWifiLock(WifiManager.WIFI_MODE_FULL, "MyWifiLock");
					PowerManager pMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
					PowerManager.WakeLock wakeLock = pMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
					
					try
					{
						wifiLock.release();
					}
					catch (Exception e)
					{

					}
					
					try
					{
						wakeLock.release();
					}
					catch (Exception e)
					{
					}

				}
			}
		});

		debug_button.setOnClickListener(new Button.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Toast.makeText(MainActivity.this, mBoundService.test_str, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void onDestroy()
	{
		Log.i("my", "app close");
		doUnbindService();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	void doBindService()
	{
		startService(new Intent(MainActivity.this, HttpService.class));
		bindService(new Intent(MainActivity.this, HttpService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
		((TextView) this.findViewById(R.id.server_status_textview)).setText("http service �Ұʤ�..");
	}

	void doUnbindService()
	{
		if (mIsBound)
		{
			unbindService(mConnection);
			mIsBound = false;
		}
		((TextView) this.findViewById(R.id.server_status_textview)).setText("http service �|���Ұ�");
	}

	private ServiceConnection mConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			mBoundService = ((HttpService.LocalBinder) service).getService();
		}
		public void onServiceDisconnected(ComponentName className)
		{
			mBoundService = null;
		}
	};

	public boolean isMyServiceRunning()
	{
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
		{
			if (HttpService.class.getName().equals(service.service.getClassName()))
			{
				Log.i("my", "service already running...");
				return true;
			}
		}
		return false;
	}

}
