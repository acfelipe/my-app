package com.felipeapp.myapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class ItemListActivity extends ListActivity implements DialogInterface.OnDismissListener {

	private static final int ADD_ITEM_OPTION = 0;
	private static final int DELETE_ITEM_OPTION = 1;
	private String roomName;
	private DatabaseOpenHelper mHelper;
	private DialogFragment mDialog;
	private ApiFacade api;
	private ApiReceiver apiReceiver;
	private LocalBroadcastManager broadcastMgr;
	private ListView lv;
	private ArrayList<String> items;
	private ArrayAdapter<String> listAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		roomName = getIntent().getStringExtra("EXTRA_ROOM_NAME");
		mHelper = new DatabaseOpenHelper(this);
		items = new ArrayList<>();
		refresh();
		lv = getListView();
		api = ApiFacade.getInstance(this);
		apiReceiver = new ApiReceiver();
		broadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
		broadcastMgr.registerReceiver(apiReceiver, apiReceiver.getFilter());
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				itemSelected(position);
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		broadcastMgr.unregisterReceiver(apiReceiver);
		mHelper.getWritableDatabase().close();
	}

	@Override
	public void onDismiss(final DialogInterface dialog) {
		refresh();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_item_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_add_item:
				showDialogFragment(ADD_ITEM_OPTION);
				return true;
			case R.id.action_delete_item:
				showDialogFragment(DELETE_ITEM_OPTION);
				return true;
			default:
				return false;
		}
	}

	// Updates the display with the items of the database
	private void refresh() {
		Cursor c = readItems();
		items = getItems(c);
		listAdapter = new ArrayAdapter<>(this, R.layout.layout_list_item, items);
		setListAdapter(listAdapter);
	}

	// Selects the dialog fragment to show when the user clicks on an item
	void showDialogFragment(int dialogID) {
		switch (dialogID) {
			case ADD_ITEM_OPTION:
				mDialog = AddItemAlertDialogFragment.newInstance();
				mDialog.show(getFragmentManager(), "Alert");
				break;
			case DELETE_ITEM_OPTION:
				mDialog = DeleteItemAlertDialogFragment.newInstance();
				mDialog.show(getFragmentManager(), "Alert");
				break;
		}
	}

	// The dialog to add new items
	public static class AddItemAlertDialogFragment extends DialogFragment {

		private DatabaseOpenHelper mHelper;
		private ArrayList<String> cores = new ArrayList<>();
		private final String[] PINS = new String[] { "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7" };

		public static AddItemAlertDialogFragment newInstance() {
			return new AddItemAlertDialogFragment();
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			mHelper = new DatabaseOpenHelper(getActivity());
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.action_add_item);
			LayoutInflater inflater = getActivity().getLayoutInflater();
			final View view = inflater.inflate(R.layout.dialog_add_item, null);

			AutoCompleteTextView coreTextView = (AutoCompleteTextView) view.findViewById(R.id.autocomplete_core);
			cores = getCores();
			ArrayAdapter<String> coreAdapter = new ArrayAdapter<>(getActivity(), R.layout.layout_list_autocomplete, cores);
			coreTextView.setAdapter(coreAdapter);

			AutoCompleteTextView pinTextView = (AutoCompleteTextView) view.findViewById(R.id.autocomplete_pin);
			ArrayAdapter<String> pinAdapter = new ArrayAdapter<>(getActivity(), R.layout.layout_list_autocomplete, PINS);
			pinTextView.setAdapter(pinAdapter);

			builder.setView(view)
					.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							String itemName = ((EditText) view.findViewById(R.id.item_name)).getText().toString();
							String coreName = ((EditText) view.findViewById(R.id.autocomplete_core)).getText().toString();
							String pin = ((EditText) view.findViewById(R.id.autocomplete_pin)).getText().toString();
							if (!addItem(itemName, coreName, pin))
								Toast.makeText(getActivity().getApplicationContext(), "O equipamento " + itemName + " já está cadastrado!", Toast.LENGTH_SHORT).show();
							else
								Toast.makeText(getActivity().getApplicationContext(), "O equipamento " + itemName + " foi cadastrado!", Toast.LENGTH_SHORT).show();
							dialog.cancel();
						}
					})
					.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			return builder.create();
		}

		private ArrayList<String> getCores() {
			String sortOrder = DatabaseOpenHelper.CORE_NAME + " ASC";
			Cursor c = mHelper.getReadableDatabase().query(
					DatabaseOpenHelper.TABLE_CORES,
					DatabaseOpenHelper.CORE_COLUMNS,
					null,
					new String[]{},
					null,
					null,
					sortOrder
			);
			ArrayList<String> coreNames = new ArrayList<>();
			if (c.moveToFirst()) {
				coreNames.add(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_NAME)));
			}
			while (c.moveToNext()) {
				coreNames.add(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_NAME)));
			}
			return coreNames;
		}

		private boolean addItem(String itemName, String coreName, String pin) {
			String roomName = ((ItemListActivity)getActivity()).getRoomName();
			String sortOrder = DatabaseOpenHelper.ITEM_NAME + " ASC";
			Cursor c = mHelper.getReadableDatabase().query(
					DatabaseOpenHelper.TABLE_ITEMS,
					DatabaseOpenHelper.ITEM_COLUMNS,
					null,
					new String[]{},
					null,
					null,
					sortOrder
			);
			if (c.moveToFirst()) {
				if (itemName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME)))&&
						roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) return false;
			}
			while (c.moveToNext()) {
				if (itemName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME)))&&
						roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) return false;
			}
			String coreId = getCoreId(coreName);
			ContentValues values = new ContentValues();
			values.put(DatabaseOpenHelper.ROOM_NAME, roomName);
			values.put(DatabaseOpenHelper.ITEM_NAME, itemName);
			values.put(DatabaseOpenHelper.CORE_ID, coreId);
			values.put(DatabaseOpenHelper.CORE_PIN, pin);
			mHelper.getWritableDatabase().insert(DatabaseOpenHelper.TABLE_ITEMS, null, values);
			return true;
		}

		private String getCoreId(String coreName) {
			String sortOrder = DatabaseOpenHelper.CORE_NAME + " ASC";
			Cursor c = mHelper.getReadableDatabase().query(
					DatabaseOpenHelper.TABLE_CORES,
					DatabaseOpenHelper.CORE_COLUMNS,
					null,
					new String[]{},
					null,
					null,
					sortOrder
			);
			if (c.moveToFirst()) {
				if (coreName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_NAME)))) {
					return c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_ID));
				}
			}
			while (c.moveToNext()) {
				if (coreName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_NAME)))) {
					return c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_ID));
				}
			}
			return null;
		}

		@Override
		public void onDismiss(final DialogInterface dialog) {
			super.onDismiss(dialog);
			mHelper.getWritableDatabase().close();
			final Activity activity = getActivity();
			if (activity instanceof DialogInterface.OnDismissListener) {
				((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
			}
		}
	}

	// The dialog to delete items
	public static class DeleteItemAlertDialogFragment extends DialogFragment {

		private DatabaseOpenHelper mHelper;
		private ArrayList<String> items = new ArrayList<>();

		public static DeleteItemAlertDialogFragment newInstance() {
			return new DeleteItemAlertDialogFragment();
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			mHelper = new DatabaseOpenHelper(getActivity().getApplicationContext());
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.action_delete_item);
			LayoutInflater inflater = getActivity().getLayoutInflater();
			final View view = inflater.inflate(R.layout.dialog_delete_item, null);

			AutoCompleteTextView coreTextView = (AutoCompleteTextView) view.findViewById(R.id.autocomplete_item);
			items = getItems();
			ArrayAdapter<String> itemAdapter = new ArrayAdapter<>(getActivity(), R.layout.layout_list_autocomplete, items);
			coreTextView.setAdapter(itemAdapter);

			builder.setView(view)
					.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							String itemName = ((EditText) view.findViewById(R.id.autocomplete_item)).getText().toString();
							if (!deleteItem(itemName))
								Toast.makeText(getActivity().getApplicationContext(), "O equipamento " + itemName + " não existe!", Toast.LENGTH_SHORT).show();
							else
								Toast.makeText(getActivity().getApplicationContext(), "O equipamento " + itemName + " foi removido!", Toast.LENGTH_SHORT).show();
							dialog.cancel();
						}
					})
					.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		}

		private ArrayList<String> getItems() {
			String roomName = ((ItemListActivity)getActivity()).getRoomName();
			String sortOrder = DatabaseOpenHelper.ITEM_NAME + " ASC";
			Cursor c = mHelper.getReadableDatabase().query(
					DatabaseOpenHelper.TABLE_ITEMS,
					DatabaseOpenHelper.ITEM_COLUMNS,
					null,
					new String[]{},
					null,
					null,
					sortOrder
			);
			ArrayList<String> itemNames = new ArrayList<>();
			if (c.moveToFirst()) {
				if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) {
					itemNames.add(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME)));
				}
			}
			while (c.moveToNext()) {
				if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) {
					itemNames.add(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME)));
				}
			}
			return itemNames;
		}

		private boolean deleteItem(String itemName) {
			String roomName = ((ItemListActivity)getActivity()).getRoomName();
			String sortOrder = DatabaseOpenHelper.ITEM_NAME + " ASC";
			Cursor c = mHelper.getReadableDatabase().query(
					DatabaseOpenHelper.TABLE_ITEMS,
					DatabaseOpenHelper.ITEM_COLUMNS,
					null,
					new String[] {},
					null,
					null,
					sortOrder
			);
			if (c.moveToFirst()) {
				if (itemName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME))) &&
						roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) {
							mHelper.getWritableDatabase().delete(DatabaseOpenHelper.TABLE_ITEMS, DatabaseOpenHelper._ID + "=?", new String[]{c.getString(c.getColumnIndex(DatabaseOpenHelper._ID))});
							return true;
						}
			}
			while (c.moveToNext()) {
				if (itemName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME))) &&
						roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) {
							mHelper.getWritableDatabase().delete(DatabaseOpenHelper.TABLE_ITEMS, DatabaseOpenHelper._ID + "=?", new String[]{c.getString(c.getColumnIndex(DatabaseOpenHelper._ID))});
							return true;
						}
			}
			return false;
		}

		@Override
		public void onDismiss(final DialogInterface dialog) {
			super.onDismiss(dialog);
			mHelper.getWritableDatabase().close();
			final Activity activity = getActivity();
			if (activity instanceof DialogInterface.OnDismissListener) {
				((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
			}
		}

	}

	// Sends a request when the user selects an item
	private void itemSelected(int position) {
		String itemName = ((TextView) lv.getChildAt(position)).getText().toString();
		Cursor c = readItems();
		String coreId = getItemCoreId(c, itemName);
		String coreAccessToken = getCoreAccessToken(coreId);
		String corePin = getItemCorePin(c, itemName);
		Log.i("Action", "Item Selected");
		Log.i("Item name",itemName);
		Log.i("Core ID", coreId);
		Log.i("Core Access Token", coreAccessToken);
		Log.i("Core pin", corePin);
		DigitalValue newValue = (((TextView) lv.getChildAt(position)).getCurrentTextColor() == Color.BLACK)
				? DigitalValue.LOW
				: DigitalValue.HIGH;
		api.digitalWrite(coreId, coreAccessToken, corePin, newValue);
	}

	// The BroadcastReceiver to get an answer from the ApiFacade class
	private class ApiReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_TINKER_RESPONSE_RECEIVED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			ParticleResponse response = intent.getParcelableExtra(ApiFacade.EXTRA_TINKER_RESPONSE);
			if (response.responseType == ParticleResponse.RESPONSE_TYPE_DIGITAL) {
				Log.i("Action","Broadcast response received");
				Log.i("Core ID", response.coreId);
				Log.i("Core pin", response.pin);
				Cursor c = readItems();
				String itemName = getItemName(c, response.coreId, response.pin);
				Log.i("Item name", itemName);
				int itemPosition = items.indexOf(itemName);
				if (((TextView) lv.getChildAt(itemPosition)).getCurrentTextColor() == Color.BLACK) {
					lv.getChildAt(itemPosition).setBackgroundResource(R.drawable.background_off);
					((TextView) lv.getChildAt(itemPosition)).setTextColor(Color.WHITE);
				} else {
					lv.getChildAt(itemPosition).setBackgroundResource(R.drawable.background_on);
					((TextView) lv.getChildAt(itemPosition)).setTextColor(Color.BLACK);
				}
			} else {
				Log.i("Invalid response type", Integer.toString(response.responseType));
			}
		}
	}

	// Returns a Cursor pointing to the table of items of the database
	private Cursor readItems() {
		String sortOrder = DatabaseOpenHelper.ITEM_NAME + " ASC";
		Cursor c = mHelper.getReadableDatabase().query(
				DatabaseOpenHelper.TABLE_ITEMS,
				DatabaseOpenHelper.ITEM_COLUMNS,
				null,
				new String[]{},
				null,
				null,
				sortOrder
		);
		return c;
	}

	// Returns the names of all items from the table in an ArrayList
	private ArrayList<String> getItems(Cursor c) {
		ArrayList<String> itemNames = new ArrayList<>();
		if (c.moveToFirst()) {
			if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) {
				itemNames.add(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME)));
			}
		}
		while (c.moveToNext()) {
			if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) {
				itemNames.add(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME)));
			}
		}
		return itemNames;
	}

	// Returns the Core ID of an item from the table given the item name
	private String getItemCoreId(Cursor c, String itemName) {
		if (c.moveToFirst()) {
			if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME))) && itemName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME)))) {
				return c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_ID));
			}
		}
		while (c.moveToNext()) {
			if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME))) && itemName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME)))) {
				return c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_ID));
			}
		}
		return null;
	}

	// Returns the Core Access Token from the cores table given the Core ID
	private String getCoreAccessToken(String coreId) {
		String sortOrder = DatabaseOpenHelper.CORE_ID + " ASC";
		Cursor c = mHelper.getReadableDatabase().query(
				DatabaseOpenHelper.TABLE_CORES,
				DatabaseOpenHelper.CORE_COLUMNS,
				null,
				new String[] {},
				null,
				null,
				sortOrder
		);
		if (c.moveToFirst()) {
			if (coreId.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_ID)))) {
				return c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_ACCESS_TOKEN));
			}
		}
		while (c.moveToNext()) {
			if (coreId.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_ID)))) {
				return c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_ACCESS_TOKEN));
			}
		}
		return null;
	}

	// Returns the Core pin of an item from the table given the item name
	private String getItemCorePin(Cursor c, String itemName) {
		if (c.moveToFirst()) {
			if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME))) && itemName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME))))
				return c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_PIN));
		}
		while (c.moveToNext()) {
			if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME))) && itemName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME))))
				return c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_PIN));
		}
		return null;
	}

	// Returns the name of an item from the table given the Core ID and the Core pin
	private String getItemName(Cursor c, String coreId, String corePin) {
		if (c.moveToFirst()) {
			if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME))) && coreId.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_ID))) &&
					corePin.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_PIN)))) {
				return c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME));
			}
		}
		while (c.moveToNext()) {
			if ( roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME))) && coreId.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_ID))) &&
					corePin.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_PIN)))) {
				return c.getString(c.getColumnIndex(DatabaseOpenHelper.ITEM_NAME));
			}
		}
		return null;
	}

	// Returns the room name of this screen
	public String getRoomName() {
		return roomName;
	}

}