import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

export type Locale = 'uk' | 'en'

const dictionaries = {
  uk: {
    appName: 'MedStorage',
    appSubtitle: 'Система управління медичними препаратами',
    loginTitle: 'Вхід у систему',
    loginSubtitle: 'Введіть ваші облікові дані для доступу',
    email: 'Email',
    password: 'Пароль',
    signIn: 'Увійти',
    noAccount: 'Немає акаунту?',
    register: 'Зареєструватися',
    forgotPassword: 'Забули пароль?',
    resetPassword: 'Відновити',
    loginNeedConfirm: 'Потрібно підтвердити пошту перед входом',
    loginInvalid: 'Невірний email або пароль',
    confirmCodeButton: 'Ввести код з листа',
    resendCode: 'Надіслати код ще раз',
    confirmationSent: 'Лист підтвердження надіслано',
    confirmationFailed: 'Не вдалося надіслати лист. Спробуйте пізніше.',
    enterEmailAbove: 'Вкажіть email у полі вище',
    theme: 'Тема',
    themeLabel: 'Тема оформлення',
    light: 'Світла',
    dark: 'Темна',
    system: 'Системна',
    notifications: 'Сповіщення',
    markAllRead: 'Прочитати всі',
    noNotifications: 'Нових сповіщень немає',
    viewAllNotifications: 'Переглянути всі',
    signOut: 'Вийти',
    dashboard: 'Дашборд',
    medicines: 'Препарати',
    storageLocations: 'Локації',
    incidents: 'Інциденти',
    devices: 'Інвентар пристроїв',
    users: 'Користувачі',
    auditLog: 'Журнал аудиту',
    adminSection: 'Адміністрування',
    systemManagement: 'Система управління',
    appFooter: '© 2026 MedStorage System',
    dashboardTitle: 'Дашборд',
    dashboardSubtitle: 'Огляд системи моніторингу медичних препаратів',
    medicinesCount: 'Препарати',
    totalSystem: 'Всього у системі',
    activeDevices: 'Активні пристрої',
    iotOnline: 'IoT датчиків онлайн',
    activeIncidents: 'Активні інциденти',
    storageViolations: 'Порушення умов зберігання',
    unreadNotifications: 'Непрочитані',
    newNotifications: 'Нові сповіщення',
    storageConditions: 'Умови зберігання',
    chooseDevice: 'Оберіть пристрій',
    activeIotDevices: 'Немає активних IoT-пристроїв',
    noChartData: 'Немає даних для відображення',
    activeIncidentsCard: 'Активні інциденти',
    activeViolations: 'активних порушень',
    noViolations: '✅ Жодних порушень',
    usersTitle: 'Управління користувачами',
    usersSubtitle: 'Перегляд та управління акаунтами вашої організації',
    addManager: 'Додати менеджера',
    createManager: 'Створити менеджера',
    role: 'Роль',
    organizationId: 'OrganizationId',
    managerDescription:
      'Менеджер матиме доступ до управління препаратами, локаціями та пристроями у межах вашої організації. Email підтверджується автоматично — менеджер отримає вітальний лист із даними для входу.',
    managerEmailHint: 'Підставляється з вашого профілю автоматично. Змінювати лише при потребі.',
    usersSectionManagers: 'Менеджери',
    usersSectionOthers: 'Інші користувачі',
    usersSectionAdmins: 'Адміністратори',
    confirmEmailTitle: 'Підтвердження email',
    confirmEmailSubtitle: 'Введіть 6-значний код, надісланий на вашу пошту',
    confirmEmailSuccess: 'Email підтверджено успішно. Тепер ви можете увійти.',
    confirmEmailError: 'Невірний код або помилка сервера',
    confirmEmailFillAll: 'Заповніть всі поля',
    yourEmail: 'Ваш Email',
    confirmationCode: 'Код підтвердження',
    backToLogin: 'Повернутися до входу',
    verify: 'Підтвердити',
    verifyLoading: 'Перевірка...',
    forgotPasswordTitle: 'Відновлення пароля',
    forgotPasswordSubtitle: 'Вкажіть email, щоб отримати 6-значний код для скидання',
    sendEmail: 'Надіслати лист',
    resetPasswordTitle: 'Скидання пароля',
    resetPasswordSubtitle: 'Введіть новий пароль',
    recoveryCode: 'Код з листа',
    newPassword: 'Новий пароль',
    confirmPassword: 'Підтвердження пароля',
    changePassword: 'Змінити пароль',
    passwordUpdated: 'Пароль оновлено успішно.',
    goToLogin: 'Перейти до входу',
  },
  en: {
    appName: 'MedStorage',
    appSubtitle: 'Medication management system',
    loginTitle: 'Sign in',
    loginSubtitle: 'Enter your credentials to continue',
    email: 'Email',
    password: 'Password',
    signIn: 'Sign in',
    noAccount: 'No account?',
    register: 'Register',
    forgotPassword: 'Forgot password?',
    resetPassword: 'Recover',
    loginNeedConfirm: 'You need to confirm your email before signing in',
    loginInvalid: 'Invalid email or password',
    confirmCodeButton: 'Enter code from email',
    resendCode: 'Resend code',
    confirmationSent: 'Confirmation email sent',
    confirmationFailed: 'Failed to send email. Try again later.',
    enterEmailAbove: 'Enter email in the field above',
    theme: 'Theme',
    themeLabel: 'Theme mode',
    light: 'Light',
    dark: 'Dark',
    system: 'System',
    notifications: 'Notifications',
    markAllRead: 'Mark all as read',
    noNotifications: 'No new notifications',
    viewAllNotifications: 'View all',
    signOut: 'Sign out',
    dashboard: 'Dashboard',
    medicines: 'Medicines',
    storageLocations: 'Locations',
    incidents: 'Incidents',
    devices: 'Device inventory',
    users: 'Users',
    auditLog: 'Audit log',
    adminSection: 'Administration',
    systemManagement: 'Management system',
    appFooter: '© 2026 MedStorage System',
    dashboardTitle: 'Dashboard',
    dashboardSubtitle: 'Overview of the medication monitoring system',
    medicinesCount: 'Medicines',
    totalSystem: 'Total in system',
    activeDevices: 'Active devices',
    iotOnline: 'IoT sensors online',
    activeIncidents: 'Active incidents',
    storageViolations: 'Storage violations',
    unreadNotifications: 'Unread',
    newNotifications: 'New notifications',
    storageConditions: 'Storage conditions',
    chooseDevice: 'Choose a device',
    activeIotDevices: 'No active IoT devices',
    noChartData: 'No data to display',
    activeIncidentsCard: 'Active incidents',
    activeViolations: 'active violations',
    noViolations: '✅ No violations',
    usersTitle: 'User management',
    usersSubtitle: 'View and manage your organization accounts',
    addManager: 'Add manager',
    createManager: 'Create manager',
    role: 'Role',
    organizationId: 'OrganizationId',
    managerDescription:
      'The manager can manage medicines, locations and devices within your organization. Email is confirmed automatically and the manager will receive a welcome email with login details.',
    managerEmailHint: 'Filled from your profile automatically. Change only if needed.',
    usersSectionManagers: 'Managers',
    usersSectionOthers: 'Other users',
    usersSectionAdmins: 'Administrators',
    confirmEmailTitle: 'Confirm email',
    confirmEmailSubtitle: 'Enter the 6-digit code sent to your email',
    confirmEmailSuccess: 'Email confirmed successfully. You can now sign in.',
    confirmEmailError: 'Invalid code or server error',
    confirmEmailFillAll: 'Fill in all fields',
    yourEmail: 'Your email',
    confirmationCode: 'Confirmation code',
    backToLogin: 'Back to sign in',
    verify: 'Confirm',
    verifyLoading: 'Verifying...',
    forgotPasswordTitle: 'Password recovery',
    forgotPasswordSubtitle: 'Enter your email to receive a 6-digit reset code',
    sendEmail: 'Send email',
    resetPasswordTitle: 'Reset password',
    resetPasswordSubtitle: 'Enter a new password',
    recoveryCode: 'Code from email',
    newPassword: 'New password',
    confirmPassword: 'Confirm password',
    changePassword: 'Change password',
    passwordUpdated: 'Password updated successfully.',
    goToLogin: 'Go to sign in',
  },
} as const

type LocaleContextValue = {
  locale: Locale
  setLocale: (locale: Locale) => void
  t: (key: string, params?: Record<string, string | number>) => string
}

const LocaleContext = createContext<LocaleContextValue>({
  locale: 'uk',
  setLocale: () => {},
  t: (key) => key,
})

function interpolate(template: string, params?: Record<string, string | number>) {
  if (!params) return template
  return Object.entries(params).reduce(
    (result, [key, value]) => result.split(`{${key}}`).join(String(value)),
    template,
  )
}

export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocale] = useState<Locale>(() => {
    const stored = localStorage.getItem('locale')
    return stored === 'en' ? 'en' : 'uk'
  })

  useEffect(() => {
    document.documentElement.lang = locale
    localStorage.setItem('locale', locale)
  }, [locale])

  const value = useMemo<LocaleContextValue>(
    () => ({
      locale,
      setLocale,
      t: (key, params) => {
        const current = dictionaries[locale] as Record<string, string>
        const fallback = dictionaries.uk as Record<string, string>
        return interpolate(current[key] ?? fallback[key] ?? key, params)
      },
    }),
    [locale],
  )

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>
}

export function useLocale() {
  return useContext(LocaleContext)
}