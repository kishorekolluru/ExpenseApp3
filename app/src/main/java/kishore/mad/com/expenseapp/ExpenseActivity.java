package kishore.mad.com.expenseapp;
/*
* InClass10
* Group 27
* */

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

import static kishore.mad.com.expenseapp.SignupActivity.REF_USERS_REF;
import static kishore.mad.com.expenseapp.SignupActivity.T_FULL_NAME;

public class ExpenseActivity extends AppCompatActivity implements ExpenseFragment.OnExpenseFragmentInteractionListener,
        AddExpenseFragment.OnFragmentInteractionListener, ShowExpenseFragment.OnFragmentInteractionListener {

    public static final String EXPENSE_FRAG = "expenseFrag";
    public static final String EXPENSE_FIREBASE_NAME = "Expenses";
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    public static String userId;
    public static String userName;
    ProgressDialog pdialog;
    ArrayList<Expense> expenseList = new ArrayList<>();
    boolean isFirst = true;
    Realm realm;
    RealmResults<Expense> expenseRealmResults;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(getIntent().getExtras().containsKey(LoginActivity.USER_ID)){
            userId = (String) getIntent().getExtras().get(LoginActivity.USER_ID);
            userName = (String) getIntent().getExtras().get(LoginActivity.FIREBASE_USER);
            TextView loginName = (TextView) findViewById(R.id.loginNameText_view);
            loginName.setText("Logged in as "+userName);
        }
        pdialog = new ProgressDialog(this);
        pdialog.setMessage("Downloading Saved Expenses...");
        pdialog.setIndeterminate(true);
//        pdialog.show();
        Realm.init(this);
        realm = Realm.getInstance(new RealmConfiguration.Builder().name("expenseRealm").build());
        expenseRealmResults = realm.where(Expense.class).findAll();
        for (Expense ex : expenseRealmResults) {
            expenseList.add(ex);
        }

        expenseRealmResults.addChangeListener(new RealmChangeListener<RealmResults<Expense>>() {
            @Override
            public void onChange(RealmResults<Expense> elements) {
                expenseList.clear();
                Log.d("demo", "The element size is " + elements.size());
                for(int i=0;i< elements.size();i++) {
                    Expense ex = elements.get(i);
                    expenseList.add(ex);
                }
//                computeCategories();
                refreshListView();
            }
        });
        getFragmentManager().beginTransaction()
                .add(R.id.container, new ExpenseFragment(), EXPENSE_FRAG).commit();
    }




    /**
     * Dispatch onStart() to all fragments.  Ensure any created loaders are
     * now started.
     */
    @Override
    protected void onStart() {
        super.onStart();
        refreshListView();
    }

    public void refreshListView(){
        if(isFirst)
            pdialog.dismiss();
        ExpenseFragment expFrag = (ExpenseFragment) getFragmentManager().findFragmentByTag(EXPENSE_FRAG);
        expFrag.displayExpenses(expenseList);
        isFirst= false;
    }

    @Override
    public void newExpenseAction() {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, new AddExpenseFragment(), "addExpenseFrag")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void deleteExpense(Expense expense) {
        for(int i =0; i<expenseRealmResults.size(); i++){
            if (expense.getId() == expenseRealmResults.get(i).getId()) {
                realm.beginTransaction();
                expenseRealmResults.deleteFromRealm(i);
                realm.commitTransaction();
            }
        }
    }

    @Override
    public void showExpense(Expense expense) {
        ShowExpenseFragment frag = new ShowExpenseFragment();
        frag.addExpense(expense);
        getFragmentManager().beginTransaction()
                .replace(R.id.container, frag, "showExpenseFrag")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public List<Expense> getFilteredExpenses(String sortBy, String filterBy) {

        String filter;
        if (sortBy.equals("Name")) {
            filter = "name";
        }else{
            filter = "amount";
        }
        RealmResults<Expense> resultant = realm.where(Expense.class).in("category", new String[]{filter}).findAllSorted(sortBy);
        List<Expense> exps = new ArrayList<>();
        for(int i =0; i<resultant.size();i++) {
            exps.add(resultant.get(i));
        }
        return exps;
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void addExpense(Expense exp) {
        //firebase code

        realm.beginTransaction();
        realm.copyToRealm(exp);
        realm.commitTransaction();
        getFragmentManager().popBackStack();

    }

    @Override
    public void cancel() {
        getFragmentManager().popBackStack();
    }

    @Override
    public void onFragmentInteraction() {
        getFragmentManager().popBackStack();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mMenuInflater = getMenuInflater();
        mMenuInflater.inflate(R.menu.activity_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.logout_menu_item:
                mAuth.signOut();
                LoginActivity.userId = "";
                initLoginActivity();
                return true;
        }
        return false;
    }
    private void initLoginActivity() {
        Intent intent = new Intent(ExpenseActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
