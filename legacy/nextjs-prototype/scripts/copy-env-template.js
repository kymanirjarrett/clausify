// ============================================
// FILE: scripts/copy-env-template.js
// ============================================
// Run with: node scripts/copy-env-template.js
// Simply copies .env.example to .env.local

import fs from 'fs';
import path from 'path';

const examplePath = path.join(__dirname, '..', '.env.example');
const localPath = path.join(__dirname, '..', '.env.local');

console.log('📋 Copying .env.example to .env.local...\n');

if (!fs.existsSync(examplePath)) {
  console.error('❌ .env.example not found!');
  console.log('Creating a basic .env.example first...');
  
  const template = `# Supabase Configuration
NEXT_PUBLIC_SUPABASE_URL=your_supabase_url_here
NEXT_PUBLIC_SUPABASE_ANON_KEY=your_supabase_anon_key_here

# AI API Keys
GROQ_API_KEY=your_groq_api_key_here
GEMINI_API_KEY=your_gemini_api_key_here

# App Configuration
NEXT_PUBLIC_APP_URL=http://localhost:3000
`;
  
  fs.writeFileSync(examplePath, template);
  console.log('✅ Created .env.example');
}

if (fs.existsSync(localPath)) {
  console.log('⚠️  .env.local already exists.');
  console.log('Please manually update it or delete it first.\n');
  process.exit(1);
}

try {
  fs.copyFileSync(examplePath, localPath);
  console.log('✅ .env.local created from template!');
  console.log('\n📝 Next steps:');
  console.log('   1. Open .env.local in your editor');
  console.log('   2. Replace placeholder values with real API keys');
  console.log('   3. Run "npm run verify-env" to check your setup');
} catch (error) {
  console.error('❌ Error copying file:', error.message);
  process.exit(1);
}
