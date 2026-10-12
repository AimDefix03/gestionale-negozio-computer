import ChangePasswordModal from '../../components/auth/ChangePasswordModal';
import SessionRenewalModal from '../../components/auth/SessionRenewalModal';
import AuthPage from '../../pages/AuthPage';
import type { SessionFlowController } from './useSessionFlow';

export function SessionEntry({ flow }: { flow: SessionFlowController }) {
  return (
    <AuthPage
      mode={flow.authMode}
      form={flow.authForm}
      passwordVisible={flow.passwordVisible}
      busy={flow.authBusy}
      message={flow.authMessage}
      onModeChange={flow.setAuthMode}
      onFormChange={flow.setAuthForm}
      onPasswordVisibilityChange={flow.setPasswordVisible}
      onSubmit={flow.submitAuthentication}
    />
  );
}

export function SessionOverlays({ flow }: { flow: SessionFlowController }) {
  return (
    <>
      {flow.renewalMessage && (
        <SessionRenewalModal
          message={flow.renewalMessage}
          password={flow.sessionPassword}
          busy={flow.renewalBusy}
          onPasswordChange={flow.setSessionPassword}
          onSubmit={flow.submitRenewal}
          onLogout={() => void flow.signOut()}
        />
      )}
      {flow.passwordChangeOpen && (
        <ChangePasswordModal
          currentPassword={flow.currentPassword}
          newPassword={flow.newPassword}
          message={flow.passwordChangeMessage}
          busy={flow.passwordChangeBusy}
          onCurrentPasswordChange={flow.setCurrentPassword}
          onNewPasswordChange={flow.setNewPassword}
          onSubmit={flow.submitPasswordChange}
          onClose={flow.closePasswordChange}
        />
      )}
    </>
  );
}
