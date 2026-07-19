/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: '#0B0D19',
        surface: '#12162C',
        surfaceVariant: '#1B203E',
        primary: '#3B82F6', // Cobalt Blue
        primaryHover: '#2563EB',
        accent: '#8B5CF6', // Purple Accent
        textPrimary: '#FFFFFF',
        textSecondary: '#94A3B8',
        borderVariant: '#2A3056'
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
