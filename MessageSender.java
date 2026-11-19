import android.util.Patterns;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

public class MessageSender {
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    public interface ResultCallback {
        void onSuccess();

        void onError(String message);
    }

    /**
     * Sends an in-app message through the Firebase callable function
     * named "sendMessage".
     *
     * For text messages, provide the text and leave mediaUrl empty.
     * For image or video messages, provide the uploaded Firebase Storage URL.
     */
    public void sendMessage(
            String recipientEmail,
            String type,
            String text,
            String mediaUrl,
            ResultCallback callback
    ) {
        if (callback == null) {
            throw new IllegalArgumentException("callback no puede ser null");
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            callback.onError("Debes iniciar sesión para enviar mensajes.");
            return;
        }

        if (recipientEmail == null
                || !Patterns.EMAIL_ADDRESS.matcher(recipientEmail.trim()).matches()) {
            callback.onError("Ingresa un correo electrónico válido.");
            return;
        }

        String normalizedEmail = recipientEmail.trim().toLowerCase();
        boolean isText = "text".equals(type);
        boolean isMedia = "image".equals(type) || "video".equals(type);

        if (!isText && !isMedia) {
            callback.onError("Tipo de mensaje no válido. Usa text, image o video.");
            return;
        }

        if (isText && (text == null || text.trim().isEmpty())) {
            callback.onError("El mensaje de texto está vacío.");
            return;
        }

        if (isMedia && (mediaUrl == null || mediaUrl.trim().isEmpty())) {
            callback.onError("Debes proporcionar la URL del archivo multimedia.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("recipientEmail", normalizedEmail);
        data.put("type", type);
        data.put("text", text == null ? "" : text.trim());
        data.put("mediaUrl", mediaUrl == null ? "" : mediaUrl.trim());

        functions.getHttpsCallable("sendMessage")
                .call(data)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(error -> {
                    String message = error.getLocalizedMessage();
                    callback.onError(message == null
                            ? "No se pudo enviar el mensaje."
                            : "No se pudo enviar el mensaje: " + message);
                });
    }
}
