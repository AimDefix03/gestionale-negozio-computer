import { ApiRequestError, SessionExpiredError } from '../api';

export function errorMessage(exception: unknown, fallback = 'Operazione non riuscita.'): string {
  if (exception instanceof ApiRequestError || exception instanceof SessionExpiredError) {
    return exception.requestId ? `${exception.message} Codice richiesta: ${exception.requestId}` : exception.message;
  }
  return exception instanceof Error ? exception.message : fallback;
}
