import { useEffect, useState } from 'react';
import { evaluatePassword, PasswordStrength } from '../api';

export default function usePasswordStrength(password: string, enabled: boolean) {
  const [passwordStrength, setPasswordStrength] = useState<PasswordStrength | null>(null);

  useEffect(() => {
    if (!enabled || !password) {
      setPasswordStrength(null);
      return;
    }

    let active = true;
    const timeout = window.setTimeout(() => {
      evaluatePassword(password)
        .then((strength) => {
          if (active) setPasswordStrength(strength);
        })
        .catch(() => {
          if (active) setPasswordStrength(null);
        });
    }, 180);

    return () => {
      active = false;
      window.clearTimeout(timeout);
    };
  }, [enabled, password]);

  return passwordStrength;
}
