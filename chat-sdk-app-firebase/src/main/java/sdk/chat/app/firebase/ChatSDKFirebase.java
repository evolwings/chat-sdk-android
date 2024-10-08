package sdk.chat.app.firebase;

import android.content.Context;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import sdk.chat.core.module.Module;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.QuickStart;
import sdk.chat.firebase.adapter.module.FirebaseModule;
import sdk.chat.firebase.push.FirebasePushModule;
import sdk.chat.firebase.ui.FirebaseUIModule;
import sdk.chat.firebase.upload.FirebaseUploadModule;
import sdk.chat.ui.extras.ExtrasModule;
import sdk.chat.ui.module.UIModule;

public class ChatSDKFirebase extends QuickStart {

    public static void quickStartWithPatreon(Context context, String rootPath, String googleMapsKey, boolean drawerEnabled, String patreonId, Module... modules) throws Exception {
        quickStart(context, rootPath, googleMapsKey, drawerEnabled, patreon(patreonId), modules);
    }

    public static void quickStartWithEmail(Context context, String rootPath, String googleMapsKey, boolean drawerEnabled, String email, Module... modules) throws Exception {
        quickStart(context, rootPath, googleMapsKey, drawerEnabled, email(email), modules);
    }

    public static void quickStartWithGithubSponsors(Context context, String rootPath, String googleMapsKey, boolean drawerEnabled, String githubId, Module... modules) throws Exception {
        quickStart(context, rootPath, googleMapsKey, drawerEnabled, github(githubId), modules);
    }

    public static void quickStart(Context context, String rootPath, String googleMapsKey, boolean drawerEnabled, Module... modules) throws Exception {
        quickStart(context, rootPath, googleMapsKey, drawerEnabled, null, modules);
    }

    /**
     * @param context
     * @param rootPath Firebase base path (can be any string, cannot contain special characters)
     * @param googleMapsKey Google static maps key.
     * @param drawerEnabled Whether to use drawer or tabs (Default)
     * @param modules Optional modules
     * @throws Exception
     */
    public static void quickStart(Context context, String rootPath, String googleMapsKey, boolean drawerEnabled, String identifier, Module... modules) throws Exception {

        List<Module> newModules = Arrays.asList(
                FirebaseModule.builder()
                        .setFirebaseRootPath(rootPath)
                        .setDisableClientProfileUpdate(false)
                        .setDevelopmentModeEnabled(true)
                        .build(),

                UIModule.builder()
                        .setPublicRoomCreationEnabled(true)
                        .setPublicRoomsEnabled(true)
                        .build(),

                FirebaseUploadModule.shared(),

                FirebasePushModule.shared(),

                ExtrasModule.builder(config -> {
                    config.setDrawerEnabled(drawerEnabled);
                }),

                FirebaseUIModule.builder()
                        .setProviders(EmailAuthProvider.PROVIDER_ID, PhoneAuthProvider.PROVIDER_ID)
                        .build()
        );

        ChatSDK.builder()
                .setGoogleMaps(googleMapsKey)
                .setAnonymousLoginEnabled(false)

                .setRemoteConfigEnabled(false)
                .setPublicChatRoomLifetimeMinutes(TimeUnit.HOURS.toMinutes(24))
                .setSendSystemMessageWhenRoleChanges(true)
                .build()

                .addModules(deduplicate(newModules, modules))

                // Activate
                .build()
                .activate(context, identifier);

    }

}
