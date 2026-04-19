/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                'minecraft-gray': '#AAAAAA',
                'minecraft-dark': '#111111',
            },
            fontFamily: {
                minecraft: ['Minecraftia', 'sans-serif'],
            },
        },
    },
    plugins: [],
}
