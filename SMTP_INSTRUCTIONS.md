# Custom SMTP Setup Guide (using Resend)

To edit your Supabase email verification templates (subjects and body text), you must set up custom SMTP. Supabase limits template edits when using the default built-in mailer.

Follow these steps to configure SMTP in your Supabase Dashboard using the Resend API Key provided.

## 1. Get SMTP Configuration Details
Here is the configuration you need to enter in your Supabase Dashboard:

- **SMTP Host**: `smtp.resend.com`
- **SMTP Port**: `465` (or `587` for TLS)
- **SMTP Username**: `resend`
- **SMTP Password**: `re_VJWJVa2s_K5LmCpXoPWiaeD2fxrDS5ueX`
- **Sender Email**: `onboarding@resend.dev` (or your verified domain sender email in Resend)
- **Sender Name**: `Watermelon`

## 2. Enter SMTP Settings in Supabase
1. Log in to your [Supabase Dashboard](https://supabase.com/dashboard).
2. Select your project: `xljlceoircpibojirxob` (or your current active project).
3. In the left sidebar, click on **Project Settings** (gear icon) -> **Auth**.
4. Scroll down to the **SMTP Settings** section.
5. Toggle **Enable Custom SMTP** to **ON**.
6. Enter the SMTP host, port, username, password, sender email, and sender name listed above.
7. Click **Save** at the bottom of the page.

## 3. Customize Your Email Templates
Once SMTP is enabled, you can customize your emails:
1. In the left sidebar, navigate to **Authentication** -> **Email Templates**.
2. Click on the template you want to edit (e.g., **Confirm Signup** or **Reset Password**).
3. You can now edit the **Subject** and **Body** text!
4. In the email body, you can address the user by username using the variable: `{{ .Data.username }}` (e.g., `Hello {{ .Data.username }}, please verify your email...`).
5. Click **Save** on each template after editing.
