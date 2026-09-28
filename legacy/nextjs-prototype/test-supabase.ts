import { supabase } from './lib/db/supabase'

async function testConnection() {
  const { data, error } = await supabase
    .from('contracts')
    .select('count')
    .limit(1)
  
  if (error) {
    console.error('Connection failed:', error)
  } else {
    console.log('✅ Supabase connected successfully!', data)
  }
}

testConnection()