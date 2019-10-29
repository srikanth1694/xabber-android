package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.StyleRes;

import com.xabber.android.R;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.utils.Utils;

public class OutgoingMessageVH extends FileMessageVH {

    OutgoingMessageVH(View itemView, MessageClickListener messageListener,
                      MessageLongClickListener longClickListener,
                      FileListener fileListener, @StyleRes int appearance) {
        super(itemView, messageListener, longClickListener, fileListener, appearance);
    }

    public void bind(MessageItem messageItem, MessagesAdapter.MessageExtraData extraData) {
        super.bind(messageItem, extraData);

        final Context context = extraData.getContext();
        boolean needTail = extraData.isNeedTail();

        setStatusIcon(messageItem);

        // setup PROGRESS
        progressBar.setVisibility(messageItem.isInProgress() ? View.VISIBLE : View.GONE);

        // setup FORWARDED
        boolean haveForwarded = messageItem.haveForwardedMessages();
        if (haveForwarded) {
            setupForwarded(messageItem, extraData);

            LinearLayout.LayoutParams forwardedParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            forwardedParams.setMargins(
                    Utils.dipToPx(1f, context),
                    Utils.dipToPx(2f, context),
                    Utils.dipToPx(needTail ? 11f : 12f, context),
                    Utils.dipToPx(0f, context));

            forwardLayout.setLayoutParams(forwardedParams);
        } else if (forwardLayout != null) forwardLayout.setVisibility(View.GONE);

        boolean imageAttached = false;
        if(messageItem.haveAttachments()) {
            if (messageItem.hasImage()) {
                imageAttached = true;
                needTail = false;
            }
        }
        // setup BACKGROUND
        Drawable shadowDrawable = context.getResources().getDrawable(
                haveForwarded ? (needTail ? R.drawable.fwd_out_shadow : R.drawable.fwd_shadow)
                        : (needTail ? R.drawable.msg_out_shadow : R.drawable.msg_shadow));
        shadowDrawable.setColorFilter(context.getResources().getColor(R.color.black), PorterDuff.Mode.MULTIPLY);
        messageBalloon.setBackgroundDrawable(context.getResources().getDrawable(
                haveForwarded ? (needTail ? R.drawable.fwd_out : R.drawable.fwd)
                            : (needTail ? R.drawable.msg_out : R.drawable.msg)));
        messageShadow.setBackgroundDrawable(shadowDrawable);

        // setup BALLOON margins
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        layoutParams.setMargins(
                Utils.dipToPx(0f, context),
                Utils.dipToPx(haveForwarded ? 0f : 2f, context),
                Utils.dipToPx(needTail ? 3f : 11f, context),
                Utils.dipToPx(2f, context));
        messageShadow.setLayoutParams(layoutParams);

        // setup MESSAGE padding
        messageBalloon.setPadding(
                Utils.dipToPx(12f, context),
                Utils.dipToPx(8f, context),
                //Utils.dipToPx(needTail ? 20f : 12f, context),
                Utils.dipToPx(needTail ? 14.5f : 6.5f, context),
                Utils.dipToPx(8f, context));

        float border = 3.5f;
        if(messageItem.haveAttachments()) {
            if(messageItem.hasImage()) {
                messageBalloon.setPadding(
                        Utils.dipToPx(border, context),
                        Utils.dipToPx(border-0.05f, context),
                        Utils.dipToPx(border, context),
                        Utils.dipToPx(border, context));
                if (messageItem.isAttachmentImageOnly()) {
                    messageTime.setTextColor(context.getResources().getColor(R.color.white));
                /*messageBalloon.setPadding(
                        Utils.dipToPx(3f, context),
                        Utils.dipToPx(-2f, context),
                        Utils.dipToPx(3f, context),
                        Utils.dipToPx(-15f, context));*/
                /*messageInfo.setPadding(
                        Utils.dipToPx(0f, context),
                        Utils.dipToPx(-7f, context),
                        Utils.dipToPx(0f, context),
                        Utils.dipToPx(0f, context));*/
                } else messageInfo.setPadding(0, 0, Utils.dipToPx(border+1.5f, context), Utils.dipToPx(4.7f, context));
            }
        }
        // setup BACKGROUND COLOR
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.message_background, typedValue, true);
        setUpMessageBalloonBackground(messageBalloon, context.getResources().getColorStateList(typedValue.resourceId));

        // subscribe for FILE UPLOAD PROGRESS
        itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                subscribeForUploadProgress(context);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                unsubscribeAll();
            }
        });
    }

    private void setStatusIcon(MessageItem messageItem) {
        statusIcon.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        boolean isFileUploadInProgress = MessageItem.isUploadFileMessage(messageItem);

        //if (isFileUploadInProgress)
        //    progressBar.setVisibility(View.VISIBLE);

        int messageIcon = R.drawable.ic_message_not_sent_14dp;

        if (messageItem.getText().equals(UPLOAD_TAG)) {
            messageIcon = 0;
            messageText.setText("");
        }

        if (!isFileUploadInProgress && !messageItem.isSent()) {
            messageIcon = R.drawable.ic_message_not_sent_14dp;
        } else if (messageItem.isDisplayed() || messageItem.isReceivedFromMessageArchive()) {
            if(messageItem.isAttachmentImageOnly())
                messageIcon = R.drawable.ic_message_displayed_image;
            else messageIcon = R.drawable.ic_message_displayed;
        } else if (messageItem.isDelivered() || messageItem.isForwarded()) {
            if(messageItem.isAttachmentImageOnly())
                messageIcon = R.drawable.ic_message_delivered_image_14dp;
            else messageIcon = R.drawable.ic_message_delivered_14dp;
        } else if (messageItem.isError()) {
            messageIcon = R.drawable.ic_message_has_error_14dp;
        } else if (messageItem.isAcknowledged()) {
            if(messageItem.isAttachmentImageOnly())
                messageIcon = R.drawable.ic_message_acknowledged_image_14dp;
            else messageIcon = R.drawable.ic_message_acknowledged_14dp;
        }

        if (messageIcon != 0) statusIcon.setImageResource(messageIcon);
        else statusIcon.setVisibility(View.GONE);
    }
}
