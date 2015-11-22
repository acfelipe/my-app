package com.felipeapp.myapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends ListActivity implements DialogInterface.OnDismissListener {

    private static final int ADD_ROOM_OPTION = 0;
    private static final int ADD_CORE_OPTION = 1;
    private static final int DELETE_ROOM_OPTION = 2;
    private static final int DELETE_CORE_OPTION = 3;
    private DialogFragment mDialog;
    private DatabaseOpenHelper mHelper;
    private ListView lv;
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new DatabaseOpenHelper(this);
        refresh();
        lv = getListView();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ItemListActivity.class);
                intent.putExtra("EXTRA_ROOM_NAME",((TextView) lv.getChildAt(position)).getText().toString());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHelper.getWritableDatabase().close();
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_room:
                showDialogFragment(ADD_ROOM_OPTION);
                return true;
            case R.id.action_add_core:
                showDialogFragment(ADD_CORE_OPTION);
                return true;
            case R.id.action_delete_room:
                showDialogFragment(DELETE_ROOM_OPTION);
                return true;
            case R.id.action_delete_core:
                showDialogFragment(DELETE_CORE_OPTION);
                return true;
            default:
                return false;
        }
    }

    // Updates the display with the rooms of the database
    private void refresh() {
        ArrayList<String> rooms = new ArrayList<>();
        String sortOrder = DatabaseOpenHelper.ROOM_NAME + " ASC";
        Cursor c = mHelper.getReadableDatabase().query(
                DatabaseOpenHelper.TABLE_ROOMS,
                DatabaseOpenHelper.ROOM_COLUMNS,
                null,
                new String[] {},
                null,
                null,
                sortOrder
        );
        if (c.moveToFirst()) {
            rooms.add(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)));
        }
        while (c.moveToNext()) {
            if (!rooms.contains(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) {
                rooms.add(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)));
            }
        }
        listAdapter = new ArrayAdapter<>(this, R.layout.layout_list_item, rooms);
        setListAdapter(listAdapter);
    }

    // Selects the dialog fragment to show when the user clicks on an item
    void showDialogFragment(int dialogID) {
        switch (dialogID) {
            case ADD_ROOM_OPTION:
                mDialog = AddRoomAlertDialogFragment.newInstance();
                mDialog.show(getFragmentManager(), "Alert");
                break;
            case ADD_CORE_OPTION:
                mDialog = AddCoreAlertDialogFragment.newInstance();
                mDialog.show(getFragmentManager(), "Alert");
                break;
            case DELETE_ROOM_OPTION:
                mDialog = DeleteRoomAlertDialogFragment.newInstance();
                mDialog.show(getFragmentManager(), "Alert");
                break;
            case DELETE_CORE_OPTION:
                mDialog = DeleteCoreAlertDialogFragment.newInstance();
                mDialog.show(getFragmentManager(), "Alert");
                break;
        }
    }

    // The dialog to add new rooms
    public static class AddRoomAlertDialogFragment extends DialogFragment {

        private DatabaseOpenHelper mHelper;

        public static AddRoomAlertDialogFragment newInstance() {
            return new AddRoomAlertDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mHelper = new DatabaseOpenHelper(getActivity().getApplicationContext());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.action_add_room);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_add_room, null);
            builder.setView(view)
                    .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            String roomName = ((EditText) view.findViewById(R.id.room_name)).getText().toString();
                            if (!addRoom(roomName))
                                Toast.makeText(getActivity().getApplicationContext(), "O ambiente " + roomName + " já está cadastrado!", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(getActivity().getApplicationContext(), "O ambiente " + roomName + " foi cadastrado!", Toast.LENGTH_SHORT).show();
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

        private boolean addRoom(String roomName) {
            String sortOrder = DatabaseOpenHelper.ROOM_NAME + " ASC";
            Cursor c = mHelper.getReadableDatabase().query(
                    DatabaseOpenHelper.TABLE_ROOMS,
                    DatabaseOpenHelper.ROOM_COLUMNS,
                    null,
                    new String[] {},
                    null,
                    null,
                    sortOrder
            );
            if (c.moveToFirst()) {
                if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) return false;
            }
            while (c.moveToNext()) {
                if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) return false;
            }
            ContentValues values = new ContentValues();
            values.put(DatabaseOpenHelper.ROOM_NAME, roomName);
            mHelper.getWritableDatabase().insert(DatabaseOpenHelper.TABLE_ROOMS, null, values);
            return true;
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

    // The dialog to add new Cores
    public static class AddCoreAlertDialogFragment extends DialogFragment {

        private DatabaseOpenHelper mHelper;

        public static AddCoreAlertDialogFragment newInstance() {
            return new AddCoreAlertDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mHelper = new DatabaseOpenHelper(getActivity().getApplicationContext());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.action_add_core);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_add_core, null);
            builder.setView(view)
                    .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            String coreName = ((EditText) view.findViewById(R.id.core_name)).getText().toString();
                            String coreId = ((EditText) view.findViewById(R.id.core_id)).getText().toString();
                            String coreAccessToken = ((EditText) view.findViewById(R.id.core_access_token)).getText().toString();
                            if (!addCore(coreName, coreId, coreAccessToken))
                                Toast.makeText(getActivity().getApplicationContext(), "O Core " + coreName + " já está cadastrado!", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(getActivity().getApplicationContext(), "O Core " + coreName + " foi cadastrado!", Toast.LENGTH_SHORT).show();
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

        private boolean addCore(String coreName, String coreId, String coreAccessToken) {
            String sortOrder = DatabaseOpenHelper.CORE_NAME + " ASC";
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
                if (coreName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_NAME)))) return false;
            }
            while (c.moveToNext()) {
                if (coreName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_NAME)))) return false;
            }
            ContentValues values = new ContentValues();
            values.put(DatabaseOpenHelper.CORE_NAME, coreName);
            values.put(DatabaseOpenHelper.CORE_ID, coreId);
            values.put(DatabaseOpenHelper.CORE_ACCESS_TOKEN, coreAccessToken);
            mHelper.getWritableDatabase().insert(DatabaseOpenHelper.TABLE_CORES, null, values);
            return true;
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

    // The dialog to delete rooms
    public static class DeleteRoomAlertDialogFragment extends DialogFragment {

        private DatabaseOpenHelper mHelper;
        private ArrayList<String> rooms = new ArrayList<>();

        public static DeleteRoomAlertDialogFragment newInstance() {
            return new DeleteRoomAlertDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mHelper = new DatabaseOpenHelper(getActivity().getApplicationContext());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.action_delete_room);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_delete_room, null);

            AutoCompleteTextView coreTextView = (AutoCompleteTextView) view.findViewById(R.id.autocomplete_room);
            rooms = getRooms();
            ArrayAdapter<String> coreAdapter = new ArrayAdapter<>(getActivity(), R.layout.layout_list_autocomplete, rooms);
            coreTextView.setAdapter(coreAdapter);

            builder.setView(view)
                    .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            String roomName = ((EditText) view.findViewById(R.id.autocomplete_room)).getText().toString();
                            if (!deleteRoom(roomName))
                                Toast.makeText(getActivity().getApplicationContext(), "O ambiente " + roomName + " não existe!", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(getActivity().getApplicationContext(), "O ambiente " + roomName + " foi removido!", Toast.LENGTH_SHORT).show();
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

        private ArrayList<String> getRooms() {
            String sortOrder = DatabaseOpenHelper.ROOM_NAME + " ASC";
            Cursor c = mHelper.getReadableDatabase().query(
                    DatabaseOpenHelper.TABLE_ROOMS,
                    DatabaseOpenHelper.ROOM_COLUMNS,
                    null,
                    new String[]{},
                    null,
                    null,
                    sortOrder
            );
            ArrayList<String> roomNames = new ArrayList<>();
            if (c.moveToFirst()) {
                roomNames.add(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)));
            }
            while (c.moveToNext()) {
                roomNames.add(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)));
            }
            return roomNames;
        }

        private boolean deleteRoom(String roomName) {
            String sortOrder = DatabaseOpenHelper.ROOM_NAME + " ASC";
            Cursor c = mHelper.getReadableDatabase().query(
                    DatabaseOpenHelper.TABLE_ROOMS,
                    DatabaseOpenHelper.ROOM_COLUMNS,
                    null,
                    new String[] {},
                    null,
                    null,
                    sortOrder
            );
            if (c.moveToFirst()) {
                if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) {
                    mHelper.getWritableDatabase().delete(DatabaseOpenHelper.TABLE_ROOMS, DatabaseOpenHelper.ROOM_NAME + "=?", new String[] {roomName});
                    return true;
                }
            }
            while (c.moveToNext()) {
                if (roomName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.ROOM_NAME)))) {
                    mHelper.getWritableDatabase().delete(DatabaseOpenHelper.TABLE_ROOMS, DatabaseOpenHelper.ROOM_NAME + "=?", new String[] {roomName});
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

    // The dialog to delete Cores
    public static class DeleteCoreAlertDialogFragment extends DialogFragment {

        private DatabaseOpenHelper mHelper;
        private ArrayList<String> cores = new ArrayList<>();

        public static DeleteCoreAlertDialogFragment newInstance() {
            return new DeleteCoreAlertDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mHelper = new DatabaseOpenHelper(getActivity().getApplicationContext());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.action_delete_core);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_delete_core, null);

            AutoCompleteTextView coreTextView = (AutoCompleteTextView) view.findViewById(R.id.autocomplete_core);
            cores = getCores();
            ArrayAdapter<String> coreAdapter = new ArrayAdapter<>(getActivity(), R.layout.layout_list_autocomplete, cores);
            coreTextView.setAdapter(coreAdapter);

            builder.setView(view)
                    .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            String coreName = ((EditText) view.findViewById(R.id.autocomplete_core)).getText().toString();
                            if (!deleteCore(coreName))
                                Toast.makeText(getActivity().getApplicationContext(), "O Core " + coreName + " não existe!", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(getActivity().getApplicationContext(), "O Core " + coreName + " foi removido!", Toast.LENGTH_SHORT).show();
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

        private boolean deleteCore(String coreName) {
            String sortOrder = DatabaseOpenHelper.CORE_NAME + " ASC";
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
                if (coreName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_NAME)))) {
                    mHelper.getWritableDatabase().delete(DatabaseOpenHelper.TABLE_CORES, DatabaseOpenHelper.CORE_NAME + "=?", new String[] {coreName});
                    return true;
                }
            }
            while (c.moveToNext()) {
                if (coreName.equals(c.getString(c.getColumnIndex(DatabaseOpenHelper.CORE_NAME)))) {
                    mHelper.getWritableDatabase().delete(DatabaseOpenHelper.TABLE_CORES, DatabaseOpenHelper.CORE_NAME + "=?", new String[] {coreName});
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

}