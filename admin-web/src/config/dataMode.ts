export const DATA_MODE: string = import.meta.env.VITE_DATA_MODE || 'real';
export const isRealMode: boolean = DATA_MODE === 'real';
export const isMockMode: boolean = DATA_MODE === 'mock';
