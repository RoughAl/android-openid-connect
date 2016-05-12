package com.lnikkila.oidcsample;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lnikkila.oidcsample.oidc.authenticator.Authenticator;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.Map;

/**
 * Initiates the login procedures and contains all UI stuff related to the main activity.
 *
 * @author Leo Nikkilä
 */
public class HomeActivity extends Activity {

    private TextView txtUserId;
    private TextView txtUsername;
    private TextView txtUserFirstname;
    private TextView txtUserLastname;
    private TextView txtUserEmail;

    private LinearLayout userInfoLayout;

    private Button loginButton;
    private ProgressBar progressBar;

    private AccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        loginButton = (Button) findViewById(R.id.loginButton);

        txtUserId = (TextView) findViewById(R.id.txtUserId);
        txtUsername = (TextView) findViewById(R.id.txtUsername);
        txtUserFirstname = (TextView) findViewById(R.id.txtUserFirstname);
        txtUserLastname = (TextView) findViewById(R.id.txtUserLastname);
        txtUserEmail = (TextView) findViewById(R.id.txtUserEmail);
        userInfoLayout = (LinearLayout)findViewById(R.id.userInfoLayout);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        accountManager = AccountManager.get(this);
    }

    /**
     * Called when the user taps the big yellow button.
     */
    public void doLogin(final View view) {
        // Grab all our accounts
        String accountType = getString(R.string.ACCOUNT_TYPE);
        final Account availableAccounts[] = accountManager.getAccountsByType(accountType);

        switch (availableAccounts.length) {
            // No account has been created, let's create one now
            case 0:
                accountManager.addAccount(accountType, Authenticator.TOKEN_TYPE_ID, null, null,
                        this, new AccountManagerCallback<Bundle>() {
                            @Override
                            public void run(AccountManagerFuture<Bundle> futureManager) {
                                // Unless the account creation was cancelled, try logging in again
                                // after the account has been created.
                                if (futureManager.isCancelled()) return;
                                doLogin(view);
                            }
                        }, null);
                break;

            // There's just one account, let's use that
            case 1:
                new ApiTask().execute(availableAccounts[0]);
                break;

            // Multiple accounts, let the user pick one
            default:
                String name[] = new String[availableAccounts.length];

                for (int i = 0; i < availableAccounts.length; i++) {
                    name[i] = availableAccounts[i].name;
                }

                new AlertDialog.Builder(this)
                        .setTitle("Choose an account")
                        .setAdapter(new ArrayAdapter<>(this,
                                        android.R.layout.simple_list_item_1, name),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int selectedAccount) {
                                        new ApiTask().execute(availableAccounts[selectedAccount]);
                                    }
                                })
                        .create()
                        .show();
        }
    }

    private class ApiTask extends AsyncTask<Account, Void, Map> {

        @Override
        protected void onPreExecute() {
            loginButton.setText("");
            progressBar.setVisibility(View.VISIBLE);
        }

        /**
         * Makes the API request. We could use the OIDCUtils.getUserInfo() method, but we'll do it
         * like this to illustrate making generic API requests after we've logged in.
         */
        @Override
        protected Map doInBackground(Account... args) {
            Account account = args[0];

            try {
                return APIUtility.getJson(HomeActivity.this, Config.userInfoUrl, account);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * Processes the API's response.
         */
        @Override
        protected void onPostExecute(Map result) {
            progressBar.setVisibility(View.INVISIBLE);

            if (result == null) {
                loginButton.setText("Couldn't get user info");
                userInfoLayout.setVisibility(View.GONE);
            } else {

                loginButton.setText("Logged in as " + result.get("preferred_username"));

                String accountType = getString(R.string.ACCOUNT_TYPE);
                Account availableAccounts[] = accountManager.getAccountsByType(accountType);

                Account account = availableAccounts[0];

                txtUserId.setText(accountManager.getUserData(account, "userinfo.userid"));
                txtUsername.setText(accountManager.getUserData(account, "userinfo.username"));
                txtUserFirstname.setText(accountManager.getUserData(account, "userinfo.firstname"));
                txtUserLastname.setText(accountManager.getUserData(account, "userinfo.lastname"));
                txtUserEmail.setText(accountManager.getUserData(account, "userinfo.email"));
                userInfoLayout.setVisibility(View.VISIBLE);
            }
        }

    }
}
