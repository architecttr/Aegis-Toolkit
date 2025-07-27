package Cleaner.Royall;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;
import android.text.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.*;
import java.io.InputStream;
import java.text.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.json.*;

public class FeedbackActivity extends Activity {
	
	private Timer _timer = new Timer();
	
	private String Random = "";
	private String RandomOutput = "";
	private String Heading = "";
	private double check = 0;
	private String cancl = "";
	private String Finall = "";
	private String Upload = "";
	
	private LinearLayout header;
	private TextView textview2;
	private LinearLayout linear2;
	private LinearLayout linear3;
	private TextView textview4;
	private EditText headline;
	private EditText details;
	private EditText gmail;
	private Button submit;
	private LinearLayout tg;
	private TextView submissions;
	private ImageView imageview1;
	private TextView textview1;
	private RadioButton bugrep;
	private RadioButton feed;
	private RadioButton featurere;
	private RadioButton other;
	private ImageView imageview2;
	private TextView textview5;
	
	private RequestNetwork bot;
	private RequestNetwork.RequestListener _bot_request_listener;
	private TimerTask cancler;
	private AlertDialog.Builder submiting;
	private AlertDialog.Builder submitor;
	private AlertDialog.Builder error;
	private AlertDialog.Builder submitted;
	private Intent i = new Intent();
	private SharedPreferences forum;
	private TimerTask t;
	private AlertDialog.Builder sbmisns;
	private AlertDialog.Builder telegramgrp;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.feedback);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		header = findViewById(R.id.header);
		textview2 = findViewById(R.id.textview2);
		linear2 = findViewById(R.id.linear2);
		linear3 = findViewById(R.id.linear3);
		textview4 = findViewById(R.id.textview4);
		headline = findViewById(R.id.headline);
		details = findViewById(R.id.details);
		gmail = findViewById(R.id.gmail);
		submit = findViewById(R.id.submit);
		tg = findViewById(R.id.tg);
		submissions = findViewById(R.id.submissions);
		imageview1 = findViewById(R.id.imageview1);
		textview1 = findViewById(R.id.textview1);
		bugrep = findViewById(R.id.bugrep);
		feed = findViewById(R.id.feed);
		featurere = findViewById(R.id.featurere);
		other = findViewById(R.id.other);
		imageview2 = findViewById(R.id.imageview2);
		textview5 = findViewById(R.id.textview5);
		bot = new RequestNetwork(this);
		submiting = new AlertDialog.Builder(this);
		submitor = new AlertDialog.Builder(this);
		error = new AlertDialog.Builder(this);
		submitted = new AlertDialog.Builder(this);
		forum = getSharedPreferences("forum", Activity.MODE_PRIVATE);
		sbmisns = new AlertDialog.Builder(this);
		telegramgrp = new AlertDialog.Builder(this);
		
		headline.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				if (_charSeq.length() > 100) {
					headline.setError("max 100 words are allowed");
					check = 1;
				}
				else {
					check = 0;
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				
			}
			
			@Override
			public void afterTextChanged(Editable _param1) {
				
			}
		});
		
		details.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				if (_charSeq.length() > 400) {
					details.setError("max 400 words are allowed");
					check = 1;
				}
				else {
					check = 0;
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				
			}
			
			@Override
			public void afterTextChanged(Editable _param1) {
				
			}
		});
		
		gmail.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				if (_charSeq.length() > 100) {
					gmail.setError("max 100 words are allowed");
					check = 1;
				}
				else {
					check = 0;
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				
			}
			
			@Override
			public void afterTextChanged(Editable _param1) {
				
			}
		});
		
		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (headline.getText().toString().length() > 2) {
					Finall = "𝗜𝗗 : ".concat(RandomOutput).concat(" •••• 𝗙𝗲𝗲𝗱𝗯𝗮𝗰𝗸 𝗧𝗶𝘁𝗹𝗲 : ".concat(headline.getText().toString().concat(" •••• 𝗚𝗺𝗮𝗶𝗹 : ".concat(gmail.getText().toString().concat(" •••• 𝗗𝗲𝘁𝗮𝗶𝗹 𝗘𝘅𝗽𝗹𝗮𝗶𝗻 : ".concat(details.getText().toString()))))));
					t = new TimerTask() {
						@Override
						public void run() {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Upload = Finall.replace("+", "¥");
								}
							});
						}
					};
					_timer.schedule(t, (int)(150));
					t = new TimerTask() {
						@Override
						public void run() {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (check == 0) {
										submitor.setTitle("𝗦𝘂𝗯𝗺𝗶𝘁 𝗙𝗲𝗲𝗱𝗯𝗮𝗰𝗸 Forum");
										submitor.setIcon(R.drawable.file);
										submitor.setPositiveButton("📤 Submit", new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface _dialog, int _which) {
												bot.startRequestNetwork(RequestNetworkController.POST, "https://api.telegram.org/botAPI/sendMessage?chat_id=@cleanerprodiscussion&text=".concat(Upload), "Telegram", _bot_request_listener);
												submiting.setTitle("𝑺𝒖𝒃𝒎𝒊𝒕𝒕𝒊𝒏𝒈 𝒀𝒐𝒖𝒓 𝑭𝒐𝒓𝒖𝒎  (5𝒔𝒆𝒄...)");
												submiting.setCancelable(false);
												final AlertDialog submitingsubmiting = submiting.create();
												submitingsubmiting.show();
												cancler = new TimerTask() {
													@Override
													public void run() {
														runOnUiThread(new Runnable() {
															@Override
															public void run() {
																if (cancl.equals("1")) {
																	submitingsubmiting.dismiss();
																	cancl = "";
																	cancler.cancel();
																}
															}
														});
													}
												};
												_timer.scheduleAtFixedRate(cancler, (int)(1000), (int)(100));
											}
										});
										submitor.setNegativeButton("No", new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface _dialog, int _which) {
												
											}
										});
										submitor.create().show();
									}
									else {
										SketchwareUtil.showMessage(getApplicationContext(), "🧑‍💻 Something is wrong in Inputs");
									}
								}
							});
						}
					};
					_timer.schedule(t, (int)(200));
				}
				else {
					SketchwareUtil.showMessage(getApplicationContext(), "🧑‍💻 Enter Something to submit");
				}
			}
		});
		
		tg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				telegramgrp.setTitle("𝗧𝗲𝗹𝗲𝗴𝗿𝗮𝗺 𝗚𝗿𝗼𝘂𝗽");
				telegramgrp.setMessage("You can check your Submission progress on Telegram group by forum id");
				telegramgrp.setIcon(R.drawable.telegrm);
				telegramgrp.setPositiveButton("Open Group", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface _dialog, int _which) {
						i.setAction(Intent.ACTION_VIEW);
						i.setData(Uri.parse("https://t.me/cleanerprodiscussion"));
						startActivity(i);
					}
				});
				telegramgrp.create().show();
			}
		});
		
		submissions.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				sbmisns.setTitle("𝗬𝗼𝘂𝗿 𝗦𝘂𝗯𝗺𝗶𝘀𝘀𝗶𝗼𝗻𝘀");
				sbmisns.setMessage("You can check its progress in Telegram Group \n".concat(forum.getString("forum", "")));
				sbmisns.setIcon(R.drawable.listj);
				sbmisns.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface _dialog, int _which) {
						
					}
				});
				sbmisns.create().show();
			}
		});
		
		bugrep.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton _param1, boolean _param2) {
				final boolean _isChecked = _param2;
				if (_isChecked) {
					featurere.setChecked(false);
					other.setChecked(false);
					feed.setChecked(false);
					Heading = "Bug Report";
				}
				else {
					
				}
			}
		});
		
		feed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton _param1, boolean _param2) {
				final boolean _isChecked = _param2;
				if (_isChecked) {
					bugrep.setChecked(false);
					featurere.setChecked(false);
					other.setChecked(false);
					Heading = "Suggestion";
				}
				else {
					
				}
			}
		});
		
		featurere.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				
			}
		});
		
		featurere.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton _param1, boolean _param2) {
				final boolean _isChecked = _param2;
				if (_isChecked) {
					bugrep.setChecked(false);
					other.setChecked(false);
					feed.setChecked(false);
					Heading = "Feature Request";
				}
				else {
					
				}
			}
		});
		
		other.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton _param1, boolean _param2) {
				final boolean _isChecked = _param2;
				if (_isChecked) {
					bugrep.setChecked(false);
					featurere.setChecked(false);
					feed.setChecked(false);
					Heading = "Others";
				}
				else {
					
				}
			}
		});
		
		_bot_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				final HashMap<String, Object> _responseHeaders = _param3;
				cancl = "1";
				if (_response.contains("{\"ok\":true,\"result\":")) {
					submitted.setTitle("𝗙𝗼𝗿𝘂𝗺 𝗦𝘂𝗯𝗺𝗶𝘁𝘁𝗲𝗱");
					submitted.setIcon(R.drawable.check);
					submitted.setCancelable(false);
					submitted.setMessage("You can check its progress in Telegram Group \n\nForum id : ".concat(RandomOutput));
					submitted.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface _dialog, int _which) {
							
						}
					});
					submitted.create().show();
					forum.edit().putString("forum", "\n••••••••••••••••••••\nFeedback Forum\nid : ".concat(RandomOutput).concat(forum.getString("forum", ""))).commit();
					submissions.setVisibility(View.VISIBLE);
					Random = "1234567890";
					RandomOutput = "";
					for (int i = 0; i < 5; i++) {
						RandomOutput = RandomOutput + Random.charAt(new java.util.Random().nextInt(Random.length()));
					}
					headline.setText("");
					gmail.setText("");
					details.setText("");
				}
				else {
					error.setTitle("𝗘𝗿𝗿𝗼𝗿");
					error.setIcon(R.drawable.icon_1);
					error.setMessage("Something went wrong Please Contact Developer on Telegram");
					error.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface _dialog, int _which) {
							
						}
					});
					error.setNegativeButton("📃 Log", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface _dialog, int _which) {
							error.setTitle(" 𝐄𝐫𝐫𝐨𝐫 𝐋𝐨𝐠");
							error.setIcon(R.drawable.info_2);
							error.setMessage(_response);
							error.setPositiveButton("OK", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface _dialog, int _which) {
									
								}
							});
							error.setNegativeButton("📃 Log", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface _dialog, int _which) {
									
								}
							});
							error.setNeutralButton("👨‍💻", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface _dialog, int _which) {
									i.setAction(Intent.ACTION_VIEW);
									i.setData(Uri.parse("https://t.me/AraafRoyall"));
									startActivity(i);
								}
							});
							error.create().show();
						}
					});
					error.setNeutralButton("👨‍💻", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface _dialog, int _which) {
							i.setAction(Intent.ACTION_VIEW);
							i.setData(Uri.parse("https://t.me/AraafRoyall"));
							startActivity(i);
						}
					});
					error.create().show();
				}
			}
			
			@Override
			public void onErrorResponse(String _param1, String _param2) {
				final String _tag = _param1;
				final String _message = _param2;
				cancl = "1";
				error.setTitle("𝗘𝗿𝗿𝗼𝗿");
				error.setIcon(R.drawable.icon_1);
				error.setMessage("Make sure you have \n• Proper Internet Connection\n• Updated App\n\nOr Contact Developer on Telegram");
				error.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface _dialog, int _which) {
						
					}
				});
				error.setNegativeButton("📃 Log", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface _dialog, int _which) {
						error.setTitle(" 𝐄𝐫𝐫𝐨𝐫 𝐋𝐨𝐠");
						error.setIcon(R.drawable.info_2);
						error.setMessage(_message);
						error.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface _dialog, int _which) {
								
							}
						});
						error.setNeutralButton("👨‍💻", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface _dialog, int _which) {
								i.setAction(Intent.ACTION_VIEW);
								i.setData(Uri.parse("https://t.me/AraafRoyall"));
								startActivity(i);
							}
						});
						error.create().show();
					}
				});
				error.setNeutralButton("👨‍💻", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface _dialog, int _which) {
						i.setAction(Intent.ACTION_VIEW);
						i.setData(Uri.parse("https://t.me/AraafRoyall"));
						startActivity(i);
					}
				});
				error.create().show();
			}
		};
	}
	
	private void initializeLogic() {
		setTitle("📜 𝗙𝗲𝗲𝗱𝗯𝗮𝗰𝗸");
		Random = "1234567890";
		RandomOutput = "";
		for (int i = 0; i < 5; i++) {
			RandomOutput = RandomOutput + Random.charAt(new java.util.Random().nextInt(Random.length()));
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		_Theme();
	}
	public void _Theme() {
		headline.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)18, (int)5, 0xFFD7CCC8, Color.TRANSPARENT));
		gmail.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)18, (int)5, 0xFFD7CCC8, Color.TRANSPARENT));
		details.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)18, (int)5, 0xFFD7CCC8, Color.TRANSPARENT));
		submit.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)30, 0xFFB9F6CA));
		tg.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFFFFFDE7));
		submissions.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFFFFFDE7));
		header.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)30, 0xFFC6FF00));
	}
	
	
	@Deprecated
	public void showMessage(String _s) {
		Toast.makeText(getApplicationContext(), _s, Toast.LENGTH_SHORT).show();
	}
	
	@Deprecated
	public int getLocationX(View _v) {
		int _location[] = new int[2];
		_v.getLocationInWindow(_location);
		return _location[0];
	}
	
	@Deprecated
	public int getLocationY(View _v) {
		int _location[] = new int[2];
		_v.getLocationInWindow(_location);
		return _location[1];
	}
	
	@Deprecated
	public int getRandom(int _min, int _max) {
		Random random = new Random();
		return random.nextInt(_max - _min + 1) + _min;
	}
	
	@Deprecated
	public ArrayList<Double> getCheckedItemPositionsToArray(ListView _list) {
		ArrayList<Double> _result = new ArrayList<Double>();
		SparseBooleanArray _arr = _list.getCheckedItemPositions();
		for (int _iIdx = 0; _iIdx < _arr.size(); _iIdx++) {
			if (_arr.valueAt(_iIdx))
			_result.add((double)_arr.keyAt(_iIdx));
		}
		return _result;
	}
	
	@Deprecated
	public float getDip(int _input) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _input, getResources().getDisplayMetrics());
	}
	
	@Deprecated
	public int getDisplayWidthPixels() {
		return getResources().getDisplayMetrics().widthPixels;
	}
	
	@Deprecated
	public int getDisplayHeightPixels() {
		return getResources().getDisplayMetrics().heightPixels;
	}
}