package com.dimonvideo.client.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchaseHistoryParams;
import com.dimonvideo.client.R;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class PurchaseHelper implements PurchasesUpdatedListener {

    public static BillingClient billingClient;
    static String product = "com.dimonvideo.client_1";

    public static void init(Context mContext, Activity activity) {

        // ---- billing init --------
        billingClient = BillingClient.newBuilder(mContext).enablePendingPurchases().setListener((billingResult, list) -> {

            Log.d("----", "Billing Client init: "+billingResult.getResponseCode());

            if (list != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for (com.android.billingclient.api.Purchase purchase : list) {
                    handlePurchase(purchase);
                }
            } else {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    Toast.makeText(mContext, mContext.getString(R.string.purchase_cancelled), Toast.LENGTH_SHORT).show();
                }


            }
        }).build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                Log.e("----", "Disconnected from the Billing Client");
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    billingClient.queryPurchaseHistoryAsync(
                            QueryPurchaseHistoryParams.newBuilder()
                                    .setProductType(BillingClient.ProductType.INAPP)
                                    .build(),
                            (billingResult1, purchasesHistoryList) -> Log.e("----", "onBillingSetupFinished "+purchasesHistoryList)
                    );

                }

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {

                    Toast.makeText(mContext, mContext.getString(R.string.no_bp), Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    public static void handlePurchase(Purchase purchase) {
        try {
            if ((purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) && isSignatureValid(purchase)) {

                if (!purchase.isAcknowledged()) {
                    AcknowledgePurchaseParams acknowledgePurchaseParams =
                            AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build();
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> Log.d("----", "success"));

                    if (billingClient!= null) {
                        billingClient.endConnection();
                    }
                }

            }
        }
        catch (Exception e)
        {
            Log.e("----", "Billing Client "+e.getMessage());
        }
    }

    private static boolean isSignatureValid(Purchase purchase) {
        return Security.verifyPurchase(Security.BASE_64_ENCODED_PUBLIC_KEY, purchase.getOriginalJson(), purchase.getSignature());
    }

    // make purchase by click
    public static void doPurchase(Activity activity){

        ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(product)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        try{
            billingClient.queryProductDetailsAsync(
                    params,
                    (billingResult, productDetailsList) -> {
                        // Process the result

                        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                                ImmutableList.of(
                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                                .setProductDetails(productDetailsList.get(0))
                                                .build()
                                );

                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(productDetailsParamsList)
                                .build();

                        billingClient.launchBillingFlow(activity, billingFlowParams);

                    }
            );
        } catch (Throwable ignored) {
            Toast.makeText(activity, activity.getString(R.string.no_bp), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();
        Log.d("----", "Billing Client code: "+responseCode);

        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null)
        {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        }
    }


}