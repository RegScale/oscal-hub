# Onboarding UX Manual Smoke Checklist

Run through this against staging once deployed. Each item is one thing to verify.

## Self-serve registration

- [ ] Sign up with a brand-new email + org name → land on dashboard.
- [ ] Receive welcome email at the registered address.
- [ ] Sign up with the same org name (different user) → see inline "name in use" error on the org-name field, no global error.

## Request-access flow

- [ ] Sign up without org name, click "Request access", submit request.
- [ ] Requester receives acknowledgment email.
- [ ] Org admin of the target org receives notification email with deep link.
- [ ] Requester landing page shows pending state plus "Or create your own organization" CTA.

## Approve / reject

- [ ] As org admin, approve the pending request → requester receives "approved" email and can use the app.
- [ ] As org admin, reject another pending request with a reason → requester receives "rejected" email containing that reason.

## Invitations

- [ ] As org admin, navigate to /org-admin/invitations.
- [ ] Send an invitation to a fresh email → row appears in the pending table.
- [ ] Recipient receives invite email with accept link.
- [ ] Click accept link as logged-out user → signup form pre-filled with email; submit completes onboarding into the inviting org.
- [ ] Click accept link as a signed-in user with the matching email → one-click acceptance lands them in the org.
- [ ] Invitation expires (set expiry < now via DB or wait) → /accept-invite?token=… shows expired state.
- [ ] As org admin, revoke a pending invitation → row removed; clicking the link shows expired state.

## Email kill switch

- [ ] Set `EMAIL_ENABLED=false` and restart backend → all flows still work; backend logs show `[email-noop] would have sent template=...` lines.

## Email rendering

- [ ] Welcome email: open in Gmail web → no broken layout.
- [ ] Invitation email: open in Outlook (or Outlook web) → button + link both work.

## Audit log

- [ ] After sending an invitation, check audit log → INVITATION_CREATED row present.
- [ ] After accepting → INVITATION_ACCEPTED row present.
- [ ] After approving an access request → AUTH-related and email audit rows correctly recorded.
