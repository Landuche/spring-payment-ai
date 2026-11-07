import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Screen } from '@/components/Screen';
import { Segmented } from '@/components/Segmented';
import { TransactionForm } from '@/components/TransactionForm';
import { TransferForm } from '@/components/TransferForm';
import { font, space, type Palette } from '@/theme/tokens';
import { useStyles } from '@/theme/theme-context';

type Mode = 'transfer' | 'deposit' | 'expense';

const MODES = [
  { value: 'transfer', label: 'Transferir' },
  { value: 'deposit', label: 'Depositar' },
  { value: 'expense', label: 'Registrar gasto' },
] as const;

export default function TransfersScreen() {
  const styles = useStyles(makeStyles);
  const [mode, setMode] = useState<Mode>('transfer');
  const [formKey, setFormKey] = useState(0);
  const reset = () => setFormKey((key) => key + 1);

  return (
    <Screen sidebar keyboardShouldPersistTaps="handled">
      <View>
        <Text style={styles.title}>Transferências</Text>
        <Text style={styles.subtitle}>Envie, deposite ou registre um gasto</Text>
      </View>

      <Segmented<Mode> value={mode} options={MODES} onChange={setMode} />

      {mode === 'transfer' ? <TransferForm key={formKey} onDone={reset} /> : null}
      {mode === 'deposit' ? <TransactionForm key={formKey} mode="deposit" onDone={reset} /> : null}
      {mode === 'expense' ? <TransactionForm key={formKey} mode="expense" onDone={reset} /> : null}
    </Screen>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    title: { color: palette.text, fontSize: font.title, fontWeight: '800' },
    subtitle: { color: palette.textMuted, fontSize: font.body, marginTop: space.xs },
  });
