import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Screen } from '@/components/Screen';
import { Segmented } from '@/components/Segmented';
import { TransactionForm } from '@/components/TransactionForm';
import { TransferForm } from '@/components/TransferForm';
import { font, space, type Palette } from '@/theme/tokens';
import { useStyles } from '@/theme/theme-context';

type Mode = 'transferir' | 'depositar' | 'gasto';

const MODES = [
  { value: 'transferir', label: 'Transferir' },
  { value: 'depositar', label: 'Depositar' },
  { value: 'gasto', label: 'Registrar gasto' },
] as const;

/**
 * Movimentações da própria conta: transferir para outra conta, depositar (entrada) ou registrar um gasto (saída).
 * Depois de concluir, o formulário é recriado do zero (nova `key`): campos limpos e uma nova chave de idempotência.
 */
export default function TransfersScreen() {
  const styles = useStyles(makeStyles);
  const [mode, setMode] = useState<Mode>('transferir');
  const [formKey, setFormKey] = useState(0);
  const reset = () => setFormKey((key) => key + 1);

  return (
    <Screen sidebar keyboardShouldPersistTaps="handled">
      <View>
        <Text style={styles.title}>Transferências</Text>
        <Text style={styles.subtitle}>Envie, deposite ou registre um gasto</Text>
      </View>

      <Segmented<Mode> value={mode} options={MODES} onChange={setMode} />

      {mode === 'transferir' ? <TransferForm key={formKey} onDone={reset} /> : null}
      {mode === 'depositar' ? <TransactionForm key={formKey} mode="deposito" onDone={reset} /> : null}
      {mode === 'gasto' ? <TransactionForm key={formKey} mode="gasto" onDone={reset} /> : null}
    </Screen>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    title: { color: palette.text, fontSize: font.title, fontWeight: '800' },
    subtitle: { color: palette.textMuted, fontSize: font.body, marginTop: space.xs },
  });
